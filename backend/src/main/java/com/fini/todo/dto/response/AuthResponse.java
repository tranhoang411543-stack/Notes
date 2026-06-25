package com.fini.todo.dto.response;

import java.util.UUID;

public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private UUID userId;
    private String username;
    private String email;

    public AuthResponse(
            String accessToken,
            String tokenType,
            UUID userId,
            String username,
            String email
    ) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.userId = userId;
        this.username = username;
        this.email = email;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }
}