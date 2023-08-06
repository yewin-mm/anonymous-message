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
 * created_date: 02/08/2023,
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
    @DocumentReference(lazy = true)
    private List<Messages> messages;

    private String loginType;
    private String role; // we can separate role table.
    private String remark;
    private LocalDateTime created;
    private LocalDateTime updated;

    private boolean deleted;


    public Users(String name, String password, boolean isOpenMessage, String loginType, String role, String remark, LocalDateTime created, LocalDateTime updated, boolean deleted) {
        this.name = name;
        this.password = password;
        this.isOpenMessage = isOpenMessage;
        this.loginType = loginType;
        this.role = role;
        this.remark = remark;
        this.created = created;
        this.updated = updated;
        this.deleted = deleted;
    }
}
