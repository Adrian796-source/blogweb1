package com.adrian.blogweb1.security.config.oauth2;


import com.adrian.blogweb1.security.config.CustomOAuth2User;
import com.adrian.blogweb1.security.config.OAuth2LoginSuccessHandler;
import com.adrian.blogweb1.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuth2LoginSuccessHandlerTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private CustomOAuth2User oAuth2User;

    @InjectMocks
    private OAuth2LoginSuccessHandler successHandler;

    @Test
    @DisplayName("Debería generar un token JWT y escribirlo en la respuesta en un login exitoso")
    void onAuthenticationSuccess_WhenAuthenticationIsValid_ShouldGenerateTokenAndWriteResponse() throws Exception {
        // --- 1. Arrange ---
        String username = "testuser";
        String email = "test@github.com";
        String fakeToken = "fake.jwt.token";

        // Configuramos el mock de la respuesta para capturar lo que se escribe
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // Simulamos la información que viene de GitHub
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getUsername()).thenReturn(username);
        when(oAuth2User.getEmail()).thenReturn(email);

        // Simulamos la carga del usuario desde nuestra base de datos
        UserDetails userDetails = new User(username, "", List.of(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        // Simulamos la creación del token
        when(jwtUtils.createToken(any(Authentication.class))).thenReturn(fakeToken);

        // --- 2. Act ---
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // --- 3. Assert ---
        // Verificamos que se estableció el tipo de contenido correcto
        verify(response).setContentType("application/json");

        // Verificamos que el JSON de respuesta contiene el token y los datos del usuario
        String responseBody = stringWriter.toString();
        assertThat(responseBody).contains("\"token\":\"Bearer " + fakeToken + "\"");
        assertThat(responseBody).contains("\"username\":\"" + username + "\"");
        assertThat(responseBody).contains("\"email\":\"" + email + "\"");
    }

    @Test
    @DisplayName("Debería devolver 500 Internal Server Error si el username de OAuth2 es nulo")
    void onAuthenticationSuccess_WhenUsernameIsNull_ShouldReturnInternalServerError() throws Exception {
        // --- 1. Arrange ---
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // Simulamos que el username que viene de GitHub es nulo
        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getUsername()).thenReturn(null);

        // --- 2. Act ---
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // --- 3. Assert ---
        // Verificamos que se estableció el código de error 500
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        // Verificamos que el cuerpo de la respuesta contiene el mensaje de error
        String responseBody = stringWriter.toString();
        assertThat(responseBody).contains("El username obtenido de GitHub es nulo");
    }

    @Test
    @DisplayName("Debería devolver 500 Internal Server Error si UserDetailsService lanza una excepción")
    void onAuthenticationSuccess_WhenUserDetailsServiceFails_ShouldReturnInternalServerError() throws Exception {
        // --- 1. Arrange ---
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        when(authentication.getPrincipal()).thenReturn(oAuth2User);
        when(oAuth2User.getUsername()).thenReturn("someuser");

        // Simulamos que el servicio falla al buscar el usuario
        when(userDetailsService.loadUserByUsername("someuser")).thenThrow(new RuntimeException("Error de base de datos simulado"));

        // --- 2. Act ---
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // --- 3. Assert ---
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String responseBody = stringWriter.toString();
        assertThat(responseBody).contains("Error de base de datos simulado");
    }
}
