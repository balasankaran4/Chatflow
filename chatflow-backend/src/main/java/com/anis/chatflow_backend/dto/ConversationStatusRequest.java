package com.anis.chatflow_backend.dto;

public record ConversationStatusRequest(String currentUserEmail, String otherUserEmail) {
}
