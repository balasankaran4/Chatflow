package com.anis.chatflow_backend.controller;

import java.util.Objects;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import com.anis.chatflow_backend.model.Message;
import com.anis.chatflow_backend.repository.MessageRepository;

@Controller

public class ChatWebSocketController {

    private final MessageRepository messageRepository;

    public ChatWebSocketController(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @MessageMapping("/sendMessage")

    @SendTo("/topic/messages")

    public Message sendMessage(
        Message message
    ) {

        messageRepository.save(Objects.requireNonNull(message));

        return message;
    }
}