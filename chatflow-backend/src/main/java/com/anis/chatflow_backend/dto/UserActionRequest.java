package com.anis.chatflow_backend.dto;

public record UserActionRequest(String currentUserEmail, String targetEmail) {
}
