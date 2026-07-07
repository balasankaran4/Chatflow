package com.anis.chatflow_backend.dto;

import java.util.List;

public record ProfileResponse(
        String name,
        String email,
        String bio,
        String profileImage,
        List<UserSummaryResponse> contacts,
        List<UserSummaryResponse> incomingRequests,
        List<UserSummaryResponse> outgoingRequests,
        List<UserSummaryResponse> blockedUsers) {
}
