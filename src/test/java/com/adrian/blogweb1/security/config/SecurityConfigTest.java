package com.adrian.blogweb1.security.config;

import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.model.UserSec;
import com.adrian.blogweb1.repository.IRoleRepository;
import com.adrian.blogweb1.repository.IUserRepository;
import com.adrian.blogweb1.security.config.props.DefaultAdminProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

// --- INICIO DE LA SOLUCIÓN FINAL ---
// Importaciones estáticas necesarias para los tests
import java.util.Collection;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
// --- FIN DE LA SOLUCIÓN FINAL ---

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.security.oauth2.client.registration.github.client-id=test-client-id",
        "spring.security.oauth2.client.registration.github.client-secret=test-client-secret"
})
class SecurityConfigTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    @DisplayName("Debería devolver 401 Unauthorized para un endpoint protegido sin autenticación")
    void requestProtectedEndpoint_withoutAuthentication_shouldReturnUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Debería devolver 403 Forbidden para un usuario autenticado sin la autoridad correcta")
    @WithMockUser
    void requestProtectedEndpoint_withAuthentication_shouldReturnForbidden() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Debería permitir el acceso al endpoint de login de OAuth2 y redirigir a GitHub")
    void requestOAuth2LoginEndpoint_shouldRedirect() throws Exception {
        mockMvc.perform(get("http://localhost/oauth2/authorization/github"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", startsWith("https://github.com/login/oauth/authorize")));
    }

    @Test
    @DisplayName("Debería rechazar una petición POST sin token CSRF")
    @WithMockUser
    void postToProtectedEndpoint_withoutCsrfToken_shouldReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/roles")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Debería aceptar una petición POST con un token CSRF válido")
    @WithMockUser(authorities = "CREATE")
    void postToProtectedEndpoint_withCsrfToken_shouldNotReturnForbidden() throws Exception {
        mockMvc.perform(post("/api/roles")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"role\":\"TEST\"}"))
                .andExpect(status().isCreated());
    }

}