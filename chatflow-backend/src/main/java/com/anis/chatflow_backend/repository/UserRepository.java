package com.anis.chatflow_backend.repository;

import com.anis.chatflow_backend.model.User;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {

    List<User> findByEmail(String email);
}