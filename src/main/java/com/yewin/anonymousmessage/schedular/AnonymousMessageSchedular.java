package com.yewin.anonymousmessage.schedular;

import com.yewin.anonymousmessage.service.AnonymousMessageService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static helper.CommonUtil.printInfo;

/**
 * author: Ye Win,
 * created_date: 08/08/2023,
 * project: anonymous-message,
 * package: com.yewin.anonymousmessage.schedular
 */

@EnableScheduling
@Service
public class AnonymousMessageSchedular {

    private final AnonymousMessageService service;

    AnonymousMessageSchedular(AnonymousMessageService service) {
        this.service = service;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "UTC") // this below method will run in UTC time 00:00:00 AM every day.
    public void process() {
        try {
            int days = 7;
            printInfo("Start deleting message scheduler process, days: "+ days + ", server run time: "+
                    LocalDateTime.now() +", UTC time: "+LocalDateTime.now(ZoneOffset.UTC));

            service.deleteMessages(days);

            printInfo("End deleting message scheduler process");
        }catch (Exception e){
            printInfo("Error in deleting message scheduler process: "+e.getMessage());
            e.printStackTrace();
        }
    }

}
