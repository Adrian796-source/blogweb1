package com.adrian.blogweb1.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor // <-- 1. Añadimos Lombok para que cree el constructor automáticamente.
public class GitHubAuthService {

    // 2. Declaramos RestTemplate como 'final' para que Lombok lo incluya en el constructor.
    private final RestTemplate restTemplate;

    @Value("${github.client.id}")
    private String clientId;

    @Value("${github.client.secret}")
    private String clientSecret;

    public String exchangeCodeForToken(String code) {
        // 3. Ya no creamos 'new RestTemplate()', usamos el que fue inyectado.
        String url = "https://github.com/login/oauth/access_token?" +
                "client_id=" + clientId +
                "&client_secret=" + clientSecret +
                "&code=" + code;

        ResponseEntity<String> response = restTemplate.postForEntity(
                url, null, String.class);

        String responseBody = response.getBody();
        if (responseBody == null || responseBody.isEmpty()) {
            throw new IllegalStateException("GitHub devolvió una respuesta vacía al intercambiar el código por el token.");
        }
        return Arrays.stream(responseBody.split("&"))
                .filter(s -> s.startsWith("access_token="))
                .findFirst()
                .map(s -> s.split("=")[1])
                .orElseThrow(() -> new IllegalStateException("El token de acceso no se encontró en la respuesta de GitHub: " + responseBody));
    }

    public Map<String, Object> getUserInfo(String accessToken) {
        // 4. También usamos aquí el RestTemplate inyectado.
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ParameterizedTypeReference<Map<String, Object>> responseType = new ParameterizedTypeReference<>() {};

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                "https://api.github.com/user",
                HttpMethod.GET,
                entity,
                responseType);

        return Optional.ofNullable(response.getBody())
                .orElseThrow(() -> new IllegalStateException("GitHub devolvió una respuesta vacía al obtener la información del usuario."));
    }
}