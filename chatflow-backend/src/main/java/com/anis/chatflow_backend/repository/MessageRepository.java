package com.anis.chatflow_backend.repository;

import com.anis.chatflow_backend.model.Message;

import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MessageRepository
        extends MongoRepository<Message, String> {

    List<Message> findBySenderAndReceiver(
            String sender,
            String receiver
    );

    List<Message> findByReceiverAndSender(
            String receiver,
            String sender
    );
}