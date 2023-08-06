package com.yewin.anonymousmessage.service;

import com.yewin.anonymousmessage.entity.Users;
import pojo.ServiceResponse;

/**
 * author: Ye Win,
 * created_date: 02/08/2023,
 * project: anonymous-message,
 * package: com.yewin.anonymousmessage.service
 */

public interface AnonymousMessageService {
    ServiceResponse register(String username, String password);
    ServiceResponse login(String username, String password);
    ServiceResponse createMessage(String userId);
    ServiceResponse sendMessage(String userId, String message, String sendBy);
}
