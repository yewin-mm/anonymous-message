package com.yewin.anonymousmessage.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * author: Ye Win,
 * created_date: 05/08/2023,
 * project: anonymous-message,
 * package: com.yewin.anonymousmessage.entity
 */

@Document(collection = "messages")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Messages {
    @Id
    private ObjectId id;
    private String message;

    private boolean showSendBy;

    private String sendBy;
    private LocalDateTime created;
    private LocalDateTime updated;
    private boolean deleted;

    public Messages(String message, boolean showSendBy, String sendBy, LocalDateTime created, LocalDateTime updated, boolean deleted) {
        this.message = message;
        this.showSendBy = showSendBy;
        this.sendBy = sendBy;
        this.created = created;
        this.updated = updated;
        this.deleted = deleted;
    }
}
