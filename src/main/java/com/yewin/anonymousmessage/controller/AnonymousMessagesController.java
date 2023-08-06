package com.yewin.anonymousmessage.controller;

import com.yewin.anonymousmessage.service.AnonymousMessageService;
import com.yewin.anonymousmessage.util.Constants;
import helper.ConstantUtil;
import helper.ResponseUtil;
import helper.ValidationUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pojo.ServiceResponse;

import java.util.List;
import java.util.Map;

import static helper.CommonUtil.printInfo;

/**
 * author: Ye Win,
 * created_date: 02/08/2023,
 * project: anonymous-message,
 * package: com.yewin.anonymousmessage.controller
 */

@RestController
@RequestMapping("/api/v1/anonymousMessage")
public class AnonymousMessagesController {

    private final AnonymousMessageService service;

    AnonymousMessagesController(AnonymousMessageService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<ServiceResponse> register(@RequestBody Map<String, String> payloads) {
        ServiceResponse serviceResponse;
        try {
            String username = payloads.get(Constants.USERNAME);
            String password = payloads.get(Constants.PASSWORD);
            printInfo("Start register process, request - username: " + username + ","+ Constants.PASSWORD+ ": " + password);
            serviceResponse = service.register(username, password);
            printInfo("End register process, response: " + serviceResponse);
            if(serviceResponse.getStatus().getStatus().equals(ConstantUtil.SUCCESS_MESSAGE)){
                return new ResponseEntity<>(serviceResponse, HttpStatus.CREATED);
            }else return new ResponseEntity<>(serviceResponse, HttpStatus.BAD_REQUEST);
        }catch (Exception e){
            e.printStackTrace();
            serviceResponse = ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, e.getMessage(), null, ConstantUtil.UTC_ZONE_ID);
            return ResponseEntity.internalServerError().body(serviceResponse);
        }

    }

    @PostMapping("/login")
    public ResponseEntity<ServiceResponse> login(@RequestBody Map<String, String> payloads) {
        ServiceResponse serviceResponse;
        try {
            String username = payloads.get(Constants.USERNAME);
            String password = payloads.get(Constants.PASSWORD);
            printInfo("Start login process, request - payloads: " + payloads);
            serviceResponse = service.login(username, password);
            printInfo("End login process, response: " + serviceResponse);
            if(serviceResponse.getStatus().getStatus().equals(ConstantUtil.SUCCESS_MESSAGE)){
                return new ResponseEntity<>(serviceResponse, HttpStatus.CREATED);
            }else return new ResponseEntity<>(serviceResponse, HttpStatus.BAD_REQUEST);
        }catch (Exception e){
            e.printStackTrace();
            serviceResponse = ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, e.getMessage(), null, ConstantUtil.UTC_ZONE_ID);
            return ResponseEntity.internalServerError().body(serviceResponse);
        }

    }


    @PostMapping("/createMessage")
    public ResponseEntity<ServiceResponse> createMessage(@RequestBody Map<String, String> payloads) {
        ServiceResponse serviceResponse;
        try {

            String username = payloads.get(Constants.USERNAME);
            String password = payloads.get(Constants.PASSWORD);
            String userId = payloads.get(Constants.USER_ID);
            printInfo("Start createMessage process, request - username: " + username + ", password: " + password);
            serviceResponse = service.createMessage(userId);
            printInfo("End createMessage process, response: " + serviceResponse);
            if(serviceResponse.getStatus().getStatus().equals(ConstantUtil.SUCCESS_MESSAGE)){
                return new ResponseEntity<>(serviceResponse, HttpStatus.CREATED);
            }else return new ResponseEntity<>(serviceResponse, HttpStatus.BAD_REQUEST);
        }catch (Exception e){
            e.printStackTrace();
            serviceResponse = ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, e.getMessage(), null, ConstantUtil.UTC_ZONE_ID);
            return ResponseEntity.internalServerError().body(serviceResponse);
        }
    }

    @PostMapping("/sendMessage")
    public ResponseEntity<ServiceResponse> sendMessage(@RequestParam String id, @RequestBody Map<String, String> payloads) {
        ServiceResponse serviceResponse;
        try {

            String message = payloads.get(Constants.MESSAGE);
            String sendBy = payloads.get(Constants.SEND_BY);
            printInfo("Start sendMessage process, request: "+ payloads);
            if(ValidationUtil.isEmptyString(sendBy)){
                sendBy = "";
            }
            serviceResponse = service.sendMessage(id, message, sendBy);
            printInfo("End sendMessage process, response: " + serviceResponse);
            if(serviceResponse.getStatus().getStatus().equals(ConstantUtil.SUCCESS_MESSAGE)){
                return new ResponseEntity<>(serviceResponse, HttpStatus.CREATED);
            }else return new ResponseEntity<>(serviceResponse, HttpStatus.BAD_REQUEST);
        }catch (Exception e){
            e.printStackTrace();
            serviceResponse = ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, e.getMessage(), null, ConstantUtil.UTC_ZONE_ID);
            return ResponseEntity.internalServerError().body(serviceResponse);
        }
    }

}
