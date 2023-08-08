package com.yewin.anonymousmessage.repository;

import com.yewin.anonymousmessage.entity.Messages;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * author: Ye Win,
 * created_date: 05/08/2023,
 * project: anonymous-message,
 * package: com.yewin.anonymousmessage.repository
 */

@Repository
public interface MessagesRepository extends MongoRepository<Messages, ObjectId> {
}
