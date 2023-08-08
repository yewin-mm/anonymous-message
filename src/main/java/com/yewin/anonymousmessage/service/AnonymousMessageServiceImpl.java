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
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * author: Ye Win,
 * created_date: 02/08/2023,
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
        user.setPassword("");
        return ResponseUtil.getResponseObj(ConstantUtil.SUCCESS_MESSAGE, null, user, ConstantUtil.UTC_ZONE_ID);
    }

    @Override
    public ServiceResponse checkName(String username) {
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

        return ResponseUtil.getResponseObj(ConstantUtil.SUCCESS_MESSAGE, null, user, ConstantUtil.UTC_ZONE_ID);
    }

    @Override
    public ServiceResponse modifyOpenMessage(String userId, boolean messageOption){
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
            // to control concurrency
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
        LocalDateTime deleteDays = LocalDateTime.now().minusDays(days);

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

        Messages messages = insertMessage(message, showSendBy, sendBy);

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
