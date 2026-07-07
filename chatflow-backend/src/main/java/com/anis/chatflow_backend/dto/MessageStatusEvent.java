package com.anis.chatflow_backend.dto;

public record MessageStatusEvent(String messageId, String sender, String receiver, boolean delivered, boolean seen) {
}
