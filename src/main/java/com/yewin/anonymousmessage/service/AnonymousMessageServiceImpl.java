package com.yewin.anonymousmessage.service;

import com.yewin.anonymousmessage.entity.Messages;
import com.yewin.anonymousmessage.entity.Users;
import com.yewin.anonymousmessage.repository.MessagesRepository;
import com.yewin.anonymousmessage.repository.UsersRepository;
import com.yewin.anonymousmessage.util.Constants;
import helper.ConstantUtil;
import helper.PasswordUtil;
import helper.ResponseUtil;
import helper.ValidationUtil;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    private final MessagesRepository messagesRepository;
    private final UsersRepository usersRepository;

    private final MongoTemplate mongoTemplate;

    AnonymousMessageServiceImpl(MessagesRepository messagesRepository, UsersRepository usersRepository, MongoTemplate mongoTemplate) {
        this.messagesRepository = messagesRepository;
        this.usersRepository = usersRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public ServiceResponse register(String username, String password) {
        if(ValidationUtil.isEmptyString(username, "username") || ValidationUtil.isEmptyString(password, "password")){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, ConstantUtil.INPUT_NULL_OR_EMPTY_MESSAGE, null, ConstantUtil.UTC_ZONE_ID);
        }
        Users user = usersRepository.insert(new Users(username, PasswordUtil.encode(password), true, "manual", "NORMAL_USER", "", LocalDateTime.now(), LocalDateTime.now(), false));
        return ResponseUtil.getResponseObj(ConstantUtil.SUCCESS_MESSAGE, null, user, ConstantUtil.UTC_ZONE_ID);
    }

    @Override
    public ServiceResponse login(String username, String password){
        if(ValidationUtil.isEmptyString(username, "username") || ValidationUtil.isEmptyString(password, "password")){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, ConstantUtil.INPUT_NULL_OR_EMPTY_MESSAGE, null, ConstantUtil.UTC_ZONE_ID);
        }
        Query query = new Query(Criteria.where("name").is(username).and("deleted").is(false));
        List<Users> userList = mongoTemplate.find(query, Users.class);
        List<Users> users = userList.stream().filter(p -> PasswordUtil.checkPassword(password, p.getPassword())).collect(Collectors.toList());
        if(users.size() != 1){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, "Username or Password is wrong!", null, ConstantUtil.UTC_ZONE_ID);
        }

        return ResponseUtil.getResponseObj(ConstantUtil.SUCCESS_MESSAGE, null, users.get(0), ConstantUtil.UTC_ZONE_ID);
    }

    @Override
    public ServiceResponse createMessage(String userId){
        if(ValidationUtil.isEmptyString(userId, "userId")){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, "User Id is null or empty", null, ConstantUtil.UTC_ZONE_ID);
        }
        Users user = mongoTemplate.findById(userId, Users.class);
        if(user==null || user.getId()==null){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, "Something went wrong", null, ConstantUtil.UTC_ZONE_ID);
        }
        String url = "localhost:8081/anonymous-message/api/v1/anonymousMessage/sendMessage?id="+user.getId();
        return ResponseUtil.getResponseObj(ConstantUtil.SUCCESS_MESSAGE, null, url, ConstantUtil.UTC_ZONE_ID);
    }

    @Override
    public ServiceResponse sendMessage(String userId, String message, String sendBy){

        ExecutorService executor = Executors.newFixedThreadPool(10);
        try {
            // to control concurrency
            CompletableFuture<ServiceResponse> future = CompletableFuture.supplyAsync(() -> sendMessageAsnyc(userId, message, sendBy), executor);
            return future.join();

        }catch (Exception e){
            e.printStackTrace();
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, e.getMessage(), null, ConstantUtil.UTC_ZONE_ID);
        }finally {
            executor.shutdown();
        }

    }

    private ServiceResponse sendMessageAsnyc(String userId, String message, String sendBy){
        if(ValidationUtil.isEmptyString(userId, "userId")){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, "User Id is null or empty", null, ConstantUtil.UTC_ZONE_ID);
        }
        if(ValidationUtil.isEmptyString(message, "message")){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, ConstantUtil.INPUT_NULL_OR_EMPTY_MESSAGE, null, ConstantUtil.UTC_ZONE_ID);
        }

        Users user = mongoTemplate.findById(userId, Users.class);
        if(user==null || user.getId()==null){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, "Link is not valid", null, ConstantUtil.UTC_ZONE_ID);
        }
        if(!user.isOpenMessage()){
            return ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, "Currently, this owner disable sending message", null, ConstantUtil.UTC_ZONE_ID);
        }

        Messages messages = messagesRepository.insert(new Messages(message, sendBy, LocalDateTime.now(), LocalDateTime.now(), false));

        mongoTemplate.update(Users.class)
                .matching(Criteria.where("id").is(userId))
                .apply(new Update().push("messages").value(messages))
                .first();

        return ResponseUtil.getResponseObj(ConstantUtil.SUCCESS_MESSAGE, null, null, ConstantUtil.UTC_ZONE_ID);
    }
}
