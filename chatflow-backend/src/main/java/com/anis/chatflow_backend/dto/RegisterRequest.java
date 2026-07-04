package com.anis.chatflow_backend.dto;

public class RegisterRequest {

    private String name;

    private String email;

    private String password;

    // NAME

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // EMAIL

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // PASSWORD

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}