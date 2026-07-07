package com.anis.chatflow_backend.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.anis.chatflow_backend.model.Message;

public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    List<Message> findByConversationIdAndReceiverHashAndDeliveredFalse(String conversationId, String receiverHash);

    List<Message> findByConversationIdAndReceiverHashAndSeenFalse(String conversationId, String receiverHash);
}
