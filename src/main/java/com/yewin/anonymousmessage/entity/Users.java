package com.yewin.anonymousmessage.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

import java.time.LocalDateTime;
import java.util.List;

/**
 * author: Ye Win,
 * created_date: 05/08/2023,
 * project: anonymous-message,
 * package: com.yewin.anonymousmessage.entity
 */

@Document(collection = "users")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Users {
    @Id
    private ObjectId id;

    @Transient
    private String userId;
    private String name;
    private String password;
    private boolean isOpenMessage;

    private String userGeneratedLink;

    @DocumentReference(lazy = true)
    private List<Messages> messages;

    private String registerType;
    private String role; // we can separate role table. As of now, this app is not focus on authorization.

    private int userLimit;
    private String remark;
    private LocalDateTime created;
    private LocalDateTime updated;

    private boolean deleted;


    public Users(String name, String password, boolean isOpenMessage, String registerType, String role, int userLimit, String remark, LocalDateTime created, LocalDateTime updated, boolean deleted) {
        this.name = name;
        this.password = password;
        this.isOpenMessage = isOpenMessage;
        this.registerType = registerType;
        this.role = role;
        this.userLimit = userLimit;
        this.remark = remark;
        this.created = created;
        this.updated = updated;
        this.deleted = deleted;
    }
}
