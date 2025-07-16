package com.adrian.blogweb1.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

// Usamos un 'record' para un DTO inmutable y conciso
public record GitHubAccessTokenResponseDTO(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("scope") String scope
) {}
