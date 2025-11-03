package com.adrian.blogweb1.utils;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        // Inyectamos valores de prueba en los campos privados anotados con @Value
        ReflectionTestUtils.setField(jwtUtils, "privateKey", "my-super-secret-key-for-testing-12345");
        ReflectionTestUtils.setField(jwtUtils, "userGenerator", "test-issuer");
        ReflectionTestUtils.setField(jwtUtils, "expirationTimeInMillis", 3600000L); // 1 hora
    }

    @Test
    @DisplayName("createToken debería generar un token JWT válido con los claims correctos")
    void createToken_shouldGenerateValidJwt() {
        // --- 1. Arrange ---
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "testuser",
                null,
                List.of(
                        new SimpleGrantedAuthority("ROLE_USER"), // Este rol se filtrará
                        new SimpleGrantedAuthority("READ"),
                        new SimpleGrantedAuthority("COMMENT")
                )
        );

        // --- 2. Act ---
        String token = jwtUtils.createToken(authentication);

        // --- 3. Assert ---
        assertThat(token).isNotNull().isNotEmpty();

        // Validamos el token para poder inspeccionar su contenido
        DecodedJWT decodedJWT = jwtUtils.validateToken(token);
        assertThat(decodedJWT.getSubject()).isEqualTo("testuser");
        assertThat(decodedJWT.getIssuer()).isEqualTo("test-issuer");
        assertThat(decodedJWT.getClaim("permissions").asList(String.class))
                .containsExactlyInAnyOrder("READ", "COMMENT");
    }

    @Test
    @DisplayName("validateToken debería lanzar una excepción para un token inválido")
    void validateToken_withInvalidToken_shouldThrowException() {
        // --- 1. Arrange ---
        String invalidToken = "this.is.not.a.valid.token";

        // --- 2. Act & 3. Assert ---
        JWTVerificationException exception = assertThrows(JWTVerificationException.class, () -> {
            jwtUtils.validateToken(invalidToken);
        });

        assertThat(exception.getMessage()).contains("Token inválido o expirado");
    }

    @Test
    @DisplayName("getSpecificClaim debería devolver el claim solicitado")
    void getSpecificClaim_shouldReturnCorrectClaim() {
        // --- 1. Arrange ---
        Authentication authentication = new UsernamePasswordAuthenticationToken("testuser", null, List.of());
        String token = jwtUtils.createToken(authentication);
        DecodedJWT decodedJWT = jwtUtils.validateToken(token);

        // --- 2. Act ---
        // Este método cubre la línea que faltaba
        Claim subjectClaim = jwtUtils.getSpecificClaim(decodedJWT, "sub");

        // --- 3. Assert ---
        assertThat(subjectClaim).isNotNull();
        assertThat(subjectClaim.asString()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("returnAllClaims debería devolver un mapa con todos los claims")
    void returnAllClaims_shouldReturnMapOfClaims() {
        // --- 1. Arrange ---
        Authentication authentication = new UsernamePasswordAuthenticationToken("testuser", null, List.of());
        String token = jwtUtils.createToken(authentication);
        DecodedJWT decodedJWT = jwtUtils.validateToken(token);

        // --- 2. Act ---
        // Este método cubre la otra línea que faltaba
        Map<String, Claim> allClaims = jwtUtils.returnAllClaims(decodedJWT);

        // --- 3. Assert ---
        assertThat(allClaims).isNotNull();
        assertThat(allClaims).hasSizeGreaterThanOrEqualTo(4); // iss, sub, iat, exp, permissions
        assertThat(allClaims.get("sub").asString()).isEqualTo("testuser");
        assertThat(allClaims.get("iss").asString()).isEqualTo("test-issuer");
    }
}
