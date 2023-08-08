package com.yewin.anonymousmessage.controller;

import com.yewin.anonymousmessage.service.AnonymousMessageService;
import com.yewin.anonymousmessage.util.Constants;
import helper.ConstantUtil;
import helper.ResponseUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pojo.ServiceResponse;

import java.util.Map;

import static com.yewin.anonymousmessage.util.Constants.*;
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
    public ResponseEntity<ServiceResponse> register(@RequestBody Map<String, String> payloads, @RequestParam(name = "userLimit", defaultValue = "500", required = false) int userLimit) {
        ServiceResponse serviceResponse;
        try {
            String username = payloads.get(Constants.USERNAME);
            String password = payloads.get(Constants.PASSWORD);
            String registerType = payloads.get(Constants.REGISTER_TYPE);
            String remark = payloads.get(Constants.REMARK);
            printInfo("Start register process, request - payloads: " + payloads);
            serviceResponse = service.register(username, password, registerType, "NORMAL_USER", userLimit, remark);
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

    @GetMapping("/checkName/{name}")
    public ResponseEntity<ServiceResponse> checkName(@PathVariable String name) {
        ServiceResponse serviceResponse;
        try {
            printInfo("Start checkName process, request - name: " + name);
            serviceResponse = service.checkName(name);
            printInfo("End checkName process, response: " + serviceResponse);
            if(serviceResponse.getStatus().getStatus().equals(ConstantUtil.SUCCESS_MESSAGE)){
                return ResponseEntity.ok().body(serviceResponse);
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
                return ResponseEntity.ok().body(serviceResponse);
            }else return new ResponseEntity<>(serviceResponse, HttpStatus.BAD_REQUEST);
        }catch (Exception e){
            e.printStackTrace();
            serviceResponse = ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, e.getMessage(), null, ConstantUtil.UTC_ZONE_ID);
            return ResponseEntity.internalServerError().body(serviceResponse);
        }

    }


    @PostMapping("/modifyOpenMessage/{messageOption}")
    public ResponseEntity<ServiceResponse> modifyOpenMessage(@RequestBody Map<String, String> payloads, @PathVariable boolean messageOption) {
        ServiceResponse serviceResponse;
        try {

            String userId = payloads.get(USER_ID);
            printInfo("Start modifyOpenMessage process, request - userId: " + userId + ", isOpenMessage: " + messageOption);
            serviceResponse = service.modifyOpenMessage(userId, messageOption);
            printInfo("End modifyOpenMessage process, response: " + serviceResponse);
            if(serviceResponse.getStatus().getStatus().equals(ConstantUtil.SUCCESS_MESSAGE)){
                return ResponseEntity.ok().body(serviceResponse);
            }else return new ResponseEntity<>(serviceResponse, HttpStatus.BAD_REQUEST);
        }catch (Exception e){
            e.printStackTrace();
            serviceResponse = ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, e.getMessage(), null, ConstantUtil.UTC_ZONE_ID);
            return ResponseEntity.internalServerError().body(serviceResponse);
        }
    }

    @PostMapping("/sendMessage")
    public ResponseEntity<ServiceResponse> sendMessage(@RequestParam(name = "showSendBy", defaultValue = "false", required = false) boolean showSendBy, @RequestBody Map<String, String> payloads) {
        ServiceResponse serviceResponse;
        try {

            String userId = payloads.get(USER_ID);
            String message = payloads.get(MESSAGE);
            String sendBy = payloads.get(SEND_BY);
            printInfo("Start sendMessage process, request: "+ payloads + ", showSendBy: "+showSendBy);
            if(!showSendBy){
                sendBy = "";
            }
            serviceResponse = service.sendMessage(userId, message, showSendBy, sendBy);
            printInfo("End sendMessage process, response: " + serviceResponse);
            if(serviceResponse.getStatus().getStatus().equals(ConstantUtil.SUCCESS_MESSAGE)){
                return ResponseEntity.ok().body(serviceResponse);
            }else return new ResponseEntity<>(serviceResponse, HttpStatus.BAD_REQUEST);
        }catch (Exception e){
            e.printStackTrace();
            serviceResponse = ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, e.getMessage(), null, ConstantUtil.UTC_ZONE_ID);
            return ResponseEntity.internalServerError().body(serviceResponse);
        }
    }

    // that is for manual delete, we will implement that delete logic my scheduler.
    @DeleteMapping("/deleteMessages")
    public ResponseEntity<ServiceResponse> deleteMessages(@RequestParam(name = "deleteDays", defaultValue = "5", required = false) int deleteDays) {
        ServiceResponse serviceResponse;
        try {

            printInfo("Start deleteMessages process, request: "+ deleteDays);
            serviceResponse = service.deleteMessages(deleteDays);
            printInfo("End deleteMessages process, response: " + serviceResponse);
            return ResponseEntity.ok().body(serviceResponse);
        }catch (Exception e){
            e.printStackTrace();
            serviceResponse = ResponseUtil.getResponseObj(ConstantUtil.FAIL_MESSAGE, e.getMessage(), null, ConstantUtil.UTC_ZONE_ID);
            return ResponseEntity.internalServerError().body(serviceResponse);
        }
    }

}
