package com.anis.chatflow_backend.dto;

public record AuthResponse(String token, UserSummaryResponse user) {
}
