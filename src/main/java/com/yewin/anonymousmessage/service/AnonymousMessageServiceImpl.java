package com.yewin.anonymousmessage.service;

import com.yewin.anonymousmessage.entity.Messages;
import com.yewin.anonymousmessage.entity.Users;
import com.yewin.anonymousmessage.repository.MessagesRepository;
import com.yewin.anonymousmessage.repository.UsersRepository;
import helper.ConstantUtil;
import helper.PasswordUtil;
import helper.ResponseUtil;
import helper.ValidationUtil;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import pojo.ServiceResponse;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * author: Ye Win,
 * created_date: 05/08/2023,
 * project: anonymous-message,
 * package: com.yewin.anonymousmessage.service
 */

@Service
public class AnonymousMessageServiceImpl implements AnonymousMessageService{

    @Value("${app.config.value.url}")
    private String sendMessageUrl;

    private final MessagesRepository messagesRepository;
    private final UsersRepository usersRepository;

    private final MongoTemplate mongoTemplate;

    AnonymousMessageServiceImpl(MessagesRepository messagesRepository, UsersRepository usersRepository, MongoTemplate mongoTemplate) {
        this.messagesRepository = messagesRepository;
        this.usersRepository = usersRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public ServiceResponse register(String username, String password, String registerType, String role, int userLimit, String remark) {
        if(ValidationUtil.isEmptyString(username, "username") || ValidationUtil.isEmptyString(password, "password")){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, ConstantUtil.INPUT_NULL_OR_EMPTY_MESSAGE, null, ConstantUtil.UTC_ZONE_ID);
        }

        List<Users> userList = retrieveUserByName(username);
        if(!userList.isEmpty()){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, ConstantUtil.RECORD_IS_EXISTED_MESSAGE, null, ConstantUtil.UTC_ZONE_ID);
        }

        Users user = insertUser(username, password, registerType, role, userLimit, remark);

        String url = sendMessageUrl+user.getId();
        updateGeneratedLinkToUsers(user.getId(), url);

        user.setUserId(String.valueOf(user.getId()));
        user.setPassword(""); // for security reasons, we don't return password back.

        // we return user information including his own Generated link after the user was registered to this system.
        // so that frontend can show that link and if user shared that link, his friends can send message by using that link.

        return ResponseUtil.getResponseObj(ConstantUtil.SUCCESS_MESSAGE, null, user, ConstantUtil.UTC_ZONE_ID);
    }

    @Override
    public ServiceResponse checkName(String username) {
        // this method is to check the name if username is already exist in the system.
        // if username is already exist in the system,
        // frontend will call this api with loop count 3 to suggest the user to choose.
        // eg. if given username is Mr.AA and if it's already exist, frontend will call with random number like Mr.AA1, Mr.AA11, Mr.AA111
        // if available (success) for checking name, frontend will suggest those name to users.

        if(ValidationUtil.isEmptyString(username, "username")){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, ConstantUtil.INPUT_NULL_OR_EMPTY_MESSAGE, null, ConstantUtil.UTC_ZONE_ID);
        }

        List<Users> userList = retrieveUserByName(username);
        if(userList.isEmpty()){
            return ResponseUtil.getResponseObj(ConstantUtil.SUCCESS_MESSAGE, null, null, ConstantUtil.UTC_ZONE_ID);
        }else
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, ConstantUtil.RECORD_IS_EXISTED_MESSAGE, null, ConstantUtil.UTC_ZONE_ID);

    }

    @Override
    public ServiceResponse login(String username, String password){
        if(ValidationUtil.isEmptyString(username, "username") || ValidationUtil.isEmptyString(password, "password")){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, ConstantUtil.INPUT_NULL_OR_EMPTY_MESSAGE, null, ConstantUtil.UTC_ZONE_ID);
        }

        List<Users> userList = retrieveUserByName(username);
        List<Users> users = userList.stream().filter(p -> PasswordUtil.checkPassword(password, p.getPassword())).collect(Collectors.toList());
        if(users.size() != 1){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, "Username or Password is wrong!", null, ConstantUtil.UTC_ZONE_ID);
        }

        Users user = users.get(0);
        user.setUserId(String.valueOf(user.getId()));
        user.setPassword("");

        // we return all messages which belong to this user in this api.
        // so, if user was shared his own link and if his friends send message through his link,
        // we will show those all message when he login to this system.

        return ResponseUtil.getResponseObj(ConstantUtil.SUCCESS_MESSAGE, null, user, ConstantUtil.UTC_ZONE_ID);
    }

    @Override
    public ServiceResponse modifyOpenMessage(String userId, boolean messageOption){
        // this method is to disable sending message,
        // owner can disable his own link means doesn't allow to send more message from anyone.

        if(ValidationUtil.isEmptyString(userId, "userId")){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, "User Id is null or empty", null, ConstantUtil.UTC_ZONE_ID);
        }

        ObjectId id = new ObjectId(userId);
        Users user = retrieveUserById(id);

        if(user==null){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, "User is not existed.", null, ConstantUtil.UTC_ZONE_ID);
        }

        mongoTemplate.update(Users.class)
                .matching(Criteria.where("id").is(userId))
                .apply(new Update()
                        .set("isOpenMessage", messageOption)
                        .set("updated", LocalDateTime.now()))
                .first();

        return ResponseUtil.getResponseObj(ConstantUtil.SUCCESS_MESSAGE, null, null, ConstantUtil.UTC_ZONE_ID);
    }

    @Override
    public ServiceResponse sendMessage(String userId, String message, boolean showSendBy, String sendBy){

        ExecutorService executor = Executors.newFixedThreadPool(10);
        try {
            // to control concurrency called multi threading because sendMessages might huge requests.
            // we used Completable future and executors with thread size 10 to handle 10 concurrent requests.
            CompletableFuture<ServiceResponse> future = CompletableFuture.supplyAsync(() -> sendMessageAsnyc(userId, message, showSendBy, sendBy), executor);
            return future.join();

        }catch (Exception e){
            e.printStackTrace();
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, e.getMessage(), null, ConstantUtil.UTC_ZONE_ID);
        }finally {
            executor.shutdown();
        }

    }

    @Override
    public ServiceResponse deleteMessages(int days){
        // this method will call from scheduler in everyday midnight 00:00:00 AM UTC time
        // to delete the message which older than 7 days to reduce db size.

        LocalDateTime deleteDays = LocalDateTime.now(ZoneOffset.UTC).minusDays(days);

        Query oldMessagesQuery = new Query(Criteria.where("created").lt(deleteDays));
        List<Messages> oldMessages = mongoTemplate.find(oldMessagesQuery, Messages.class);

        for (Messages oldMessage : oldMessages) {
            Update update = new Update().pull("messages", oldMessage);
            mongoTemplate.updateMulti(new Query(), update, Users.class);
        }

        return ResponseUtil.getResponseObj(ConstantUtil.SUCCESS_MESSAGE, null, null, ConstantUtil.UTC_ZONE_ID);

    }

    private ServiceResponse sendMessageAsnyc(String userId, String message, boolean showSendBy, String sendBy){
        if(ValidationUtil.isEmptyString(userId, "userId")){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, "User Id is null or empty", null, ConstantUtil.UTC_ZONE_ID);
        }
        if(ValidationUtil.isEmptyString(message, "message")){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, ConstantUtil.INPUT_NULL_OR_EMPTY_MESSAGE, null, ConstantUtil.UTC_ZONE_ID);
        }

        ObjectId id = new ObjectId(userId);
        Users user = retrieveUserById(id);

        if(user==null){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, "User is not existed.", null, ConstantUtil.UTC_ZONE_ID);
        }

        if(!user.isOpenMessage()){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, "This owner is currently disabled from sending messages.", null, ConstantUtil.UTC_ZONE_ID);
        }

        /**
         * we can filter the user limit by using user.getUserLimit()
         * to stop allowing the sending messages from many users and request the money for extra sending message.
         */

        Messages messages = insertMessage(message, showSendBy, sendBy); // we need to encrypt the message later

        updateMessageListToUsers(new ObjectId(userId), messages);

        return ResponseUtil.getResponseObj(ConstantUtil.SUCCESS_MESSAGE, null, null, ConstantUtil.UTC_ZONE_ID);
    }


    private List<Users> retrieveUserByName(String username) {
        Query query = new Query(Criteria.where("name").is(username).and("deleted").is(false));
        return mongoTemplate.find(query, Users.class);
    }

    private Users retrieveUserById(ObjectId userId) {
        Query query = new Query(Criteria.where("id").is(userId).and("deleted").is(false));
        return mongoTemplate.findOne(query, Users.class);
    }

    private Users insertUser(String username, String password, String registerType, String role, int userLimit, String remark) {
        Users user = new Users(username, PasswordUtil.encode(password), true,  registerType, role, userLimit, remark, LocalDateTime.now(), LocalDateTime.now(), false);
        return usersRepository.insert(user);
    }

    private Messages insertMessage(String message, boolean showSendBy, String sendBy) {
        Messages messages = new Messages(message, showSendBy, sendBy, LocalDateTime.now(), LocalDateTime.now(), false);
        return messagesRepository.insert(messages);
    }

    private void updateGeneratedLinkToUsers(ObjectId userId, String value) {
        mongoTemplate.update(Users.class)
                .matching(Criteria.where("id").is(userId))
                .apply(new Update().set("userGeneratedLink", value)) // set method will override existing value or create new file if not existed.
                .first();
    }

    private void updateMessageListToUsers(ObjectId userId, Messages messages){
        mongoTemplate.update(Users.class)
                .matching(Criteria.where("id").is(userId))
                .apply(new Update().push("messages").value(messages))// push method append values as array list.
                .first();
    }


}
