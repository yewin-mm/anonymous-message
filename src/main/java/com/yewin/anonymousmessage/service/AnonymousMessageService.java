package com.yewin.anonymousmessage.service;

import com.yewin.anonymousmessage.entity.Users;
import pojo.ServiceResponse;

/**
 * author: Ye Win,
 * created_date: 05/08/2023,
 * project: anonymous-message,
 * package: com.yewin.anonymousmessage.service
 */

public interface AnonymousMessageService {
    ServiceResponse register(String username, String password, String registerType, String role, int userLimit, String remark);
    ServiceResponse checkName(String username);
    ServiceResponse login(String username, String password);
    ServiceResponse modifyOpenMessage(String userId, boolean messageOption);
    ServiceResponse sendMessage(String userId, String message, boolean showSendBy, String sendBy);

    ServiceResponse deleteMessages(int days);
}
