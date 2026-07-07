package com.anis.chatflow_backend.dto;

public record UserSummaryResponse(
        String name,
        String email,
        String bio,
        String profileImage,
        boolean contact,
        boolean incomingRequest,
        boolean outgoingRequest,
        boolean blocked,
        boolean blockedBy,
        boolean canChat) {
}
