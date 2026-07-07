package com.anis.chatflow_backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.anis.chatflow_backend.model.User;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmailHash(String emailHash);

    List<User> findByEmailHashIn(Collection<String> emailHashes);
}
