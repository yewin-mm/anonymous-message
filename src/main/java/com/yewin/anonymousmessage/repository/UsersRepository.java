package com.yewin.anonymousmessage.repository;

import com.yewin.anonymousmessage.entity.Users;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 * author: Ye Win,
 * created_date: 02/08/2023,
 * project: anonymous-message,
 * package: com.yewin.anonymousmessage.repository
 */

@Repository
public interface UsersRepository extends MongoRepository<Users, ObjectId> {

}
