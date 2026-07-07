package com.anis.chatflow_backend.dto;

public record ProfileUpdateRequest(String email, String name, String bio, String profileImage) {
}
