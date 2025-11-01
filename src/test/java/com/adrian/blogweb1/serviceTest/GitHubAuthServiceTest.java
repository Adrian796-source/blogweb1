package com.adrian.blogweb1.serviceTest;

import com.adrian.blogweb1.service.GitHubAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubAuthServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private GitHubAuthService gitHubAuthService;

    @BeforeEach
    void setUp() {
        // Creamos manualmente la instancia del servicio, pasándole el mock de RestTemplate.
        // Esto nos da control total y asegura que no se hagan llamadas reales a la red.
        gitHubAuthService = new GitHubAuthService(restTemplate);

        // Inyectamos manualmente los valores de las propiedades @Value para los tests.
        ReflectionTestUtils.setField(gitHubAuthService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(gitHubAuthService, "clientSecret", "test-client-secret");
    }

    @Test
    @DisplayName("exchangeCodeForToken debería devolver un token cuando la respuesta de GitHub es válida")
    void exchangeCodeForToken_WhenResponseIsValid_ShouldReturnToken() {
        // --- 1. Arrange ---
        String code = "valid-code";
        String githubResponse = "access_token=gho_faketoken&scope=user&token_type=bearer";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(githubResponse, HttpStatus.OK);

        // Simulamos la llamada POST del RestTemplate
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenReturn(responseEntity);

        // --- 2. Act ---
        String token = gitHubAuthService.exchangeCodeForToken(code);

        // --- 3. Assert ---
        assertThat(token).isEqualTo("gho_faketoken");
    }

    @Test
    @DisplayName("exchangeCodeForToken debería lanzar una excepción si la respuesta no contiene el token")
    void exchangeCodeForToken_WhenTokenIsMissing_ShouldThrowException() {
        // --- 1. Arrange ---
        String code = "invalid-code";
        String errorResponse = "error=bad_verification_code&error_description=The+code+passed+is+incorrect.";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(errorResponse, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(), eq(String.class))).thenReturn(responseEntity);

        // --- 2. Act & 3. Assert ---
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            gitHubAuthService.exchangeCodeForToken(code);
        });

        assertThat(exception.getMessage()).contains("El token de acceso no se encontró en la respuesta de GitHub");
    }

    @Test
    @DisplayName("exchangeCodeForToken debería lanzar una excepción si el cuerpo de la respuesta es nulo")
    void exchangeCodeForToken_WhenBodyIsNull_ShouldThrowException() {
        // --- 1. Arrange ---
        String code = "some-code";
        // Simulamos una respuesta con cuerpo nulo
        ResponseEntity<String> nullBodyResponse = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(nullBodyResponse);

        // --- 2. Act & 3. Assert ---
        // Verificamos que se lanza la excepción correcta
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            gitHubAuthService.exchangeCodeForToken(code);
        });

        assertThat(exception.getMessage()).contains("GitHub devolvió una respuesta vacía");
    }

    @Test
    @DisplayName("getUserInfo debería devolver la información del usuario cuando el token es válido")
    void getUserInfo_WhenTokenIsValid_ShouldReturnUserInfo() {
        // --- 1. Arrange ---
        String accessToken = "valid-access-token";
        Map<String, Object> userInfo = Map.of("login", "testuser", "id", 12345);
        ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(userInfo, HttpStatus.OK);

        // Simulamos la llamada GET del RestTemplate
        when(restTemplate.exchange(
                eq("https://api.github.com/user"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        // --- 2. Act ---
        Map<String, Object> result = gitHubAuthService.getUserInfo(accessToken);

        // --- 3. Assert ---
        assertThat(result).isNotNull();
        assertThat(result.get("login")).isEqualTo("testuser");
    }

    @Test
    @DisplayName("getUserInfo debería lanzar una excepción si la respuesta de GitHub es nula")
    void getUserInfo_WhenResponseIsNull_ShouldThrowException() {
        // --- 1. Arrange ---
        String accessToken = "valid-access-token";
        // Simulamos una respuesta con cuerpo nulo
        ResponseEntity<Map<String, Object>> nullResponseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(nullResponseEntity);

        // --- 2. Act & 3. Assert ---
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            gitHubAuthService.getUserInfo(accessToken);
        });

        assertThat(exception.getMessage()).contains("GitHub devolvió una respuesta vacía al obtener la información del usuario.");
    }
}