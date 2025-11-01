package com.adrian.blogweb1.controller;

import com.adrian.blogweb1.security.config.CustomOAuth2User;
import com.adrian.blogweb1.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class OAuth2Controller {


    private final JwtUtils jwtUtils;

    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> getUserInfo(
            @AuthenticationPrincipal CustomOAuth2User principal,
            Principal fallbackPrincipal) {

        if (principal != null) {
            return ResponseEntity.ok(Map.of(
                    "username", principal.getUsername(),
                    "email", principal.getEmail(),
                    "roles", principal.getAuthorities().stream()
                            .map(auth -> auth.getAuthority())
                            .toList(),
                    "authType", "OAUTH2"
            ));
        } else if (fallbackPrincipal != null) {
            return ResponseEntity.ok(Map.of(
                    "username", fallbackPrincipal.getName(),
                    "authType", "OTHER"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "message", "No autenticado",
                "isAuthenticated", false
        ));
    }

    @GetMapping("/generate-jwt")
    public ResponseEntity<Map<String, String>> generateJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Debes autenticarte primero"
            ));
        }

        String token = jwtUtils.createToken(authentication);

        return ResponseEntity.ok(Map.of(
                "token", token,
                "tokenType", "Bearer"
        ));
    }
}