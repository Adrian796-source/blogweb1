package com.adrian.blogweb1.security.config.filter;

import com.adrian.blogweb1.utils.JwtUtils;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenValidatorTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtTokenValidator jwtTokenValidator;

    // Limpiamos el contexto de seguridad después de cada test para evitar interferencias
    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Debería continuar la cadena de filtros si no hay cabecera de autorización")
    void doFilterInternal_whenNoAuthHeader_shouldContinueFilterChain() throws ServletException, IOException {
        // --- 1. Arrange ---
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // --- 2. Act ---
        jwtTokenValidator.doFilterInternal(request, response, filterChain);

        // --- 3. Assert ---
        // Verificamos que se llamó al siguiente filtro en la cadena
        verify(filterChain, times(1)).doFilter(request, response);
        // Verificamos que no se estableció ninguna autenticación
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Debería continuar la cadena de filtros si la cabecera no es de tipo Bearer")
    void doFilterInternal_whenHeaderIsNotBearer_shouldContinueFilterChain() throws ServletException, IOException {
        // --- 1. Arrange ---
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz"); // Un token Basic64
        MockHttpServletResponse response = new MockHttpServletResponse();

        // --- 2. Act ---
        jwtTokenValidator.doFilterInternal(request, response, filterChain);

        // --- 3. Assert ---
        verify(filterChain, times(1)).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("Debería establecer la autenticación si el token JWT es válido")
    void doFilterInternal_whenTokenIsValid_shouldSetAuthentication() throws ServletException, IOException {
        // --- 1. Arrange ---
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        DecodedJWT decodedJWT = mock(DecodedJWT.class);
        Claim rolesClaim = mock(Claim.class);
        Claim permissionsClaim = mock(Claim.class);

        when(jwtUtils.validateToken("valid.jwt.token")).thenReturn(decodedJWT);
        when(jwtUtils.extractUsername(decodedJWT)).thenReturn("testuser");
        when(decodedJWT.getClaim("roles")).thenReturn(rolesClaim);
        when(decodedJWT.getClaim("permissions")).thenReturn(permissionsClaim);
        when(rolesClaim.asList(String.class)).thenReturn(List.of("ADMIN"));
        when(permissionsClaim.asList(String.class)).thenReturn(List.of("CREATE", "READ"));

        // --- 2. Act ---
        jwtTokenValidator.doFilterInternal(request, response, filterChain);

        // --- 3. Assert ---
        // Verificamos que la autenticación se estableció en el contexto
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("testuser");
        assertThat(authentication.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_ADMIN", "CREATE", "READ");

        // Verificamos que la cadena de filtros continuó
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("Debería devolver 401 Unauthorized si el token JWT es inválido")
    void doFilterInternal_whenTokenIsInvalid_shouldReturnUnauthorized() throws ServletException, IOException {
        // --- 1. Arrange ---
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // Simulamos que la validación del token lanza una excepción
        when(jwtUtils.validateToken("invalid.jwt.token")).thenThrow(new JWTVerificationException("Token expirado"));

        // --- 2. Act ---
        jwtTokenValidator.doFilterInternal(request, response, filterChain);

        // --- 3. Assert ---
        // Verificamos que la respuesta es 401 Unauthorized
        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_UNAUTHORIZED);
        assertThat(response.getContentAsString()).contains("Token inválido o expirado");

        // Verificamos que la cadena de filtros NO continuó después del error
        verify(filterChain, never()).doFilter(request, response);
    }
}