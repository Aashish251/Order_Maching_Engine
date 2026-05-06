// src/main/java/com/project/ome/api/dto/auth/AuthResponse.java
package com.project.ome.api.dto.auth;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private final String accessToken;
    private final String tokenType;      // always "Bearer"
    private final long   expiresIn;      // seconds
    private final String role;
}