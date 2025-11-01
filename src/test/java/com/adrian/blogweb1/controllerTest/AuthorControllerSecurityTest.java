package com.adrian.blogweb1.controllerTest;



import com.adrian.blogweb1.dto.AuthorCreateRequestDTO;
import com.adrian.blogweb1.service.IAuthorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 1. Usamos @SpringBootTest para cargar el contexto completo, incluyendo la seguridad.
@SpringBootTest
@AutoConfigureMockMvc
class AuthorControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // --- INICIO DE LA CORRECCIÓN PARA SONARQUBE ---
    // 1. Definimos una configuración de prueba anidada.
    @TestConfiguration
    static class AuthorServiceTestConfig {
        // 2. Creamos un mock de IAuthorService y lo registramos como un bean.
        //    @Primary asegura que este mock sea el que se use, sobreescribiendo
        //    la implementación real si existiera en el contexto.
        @Bean
        @Primary
        public IAuthorService mockAuthorService() {
            return Mockito.mock(IAuthorService.class);
        }
    }
    // --- FIN DE LA CORRECCIÓN ---

    @Test
    @DisplayName("GET /api/authors - Debería devolver 401 Unauthorized si no está autenticado")
    void getAuthors_WhenUnauthenticated_ShouldReturn401() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/authors"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/authors - Debería devolver 403 Forbidden si no tiene el permiso 'CREATE'")
    @WithMockUser(authorities = "READ") // Usuario autenticado pero con permisos insuficientes
    void createAuthor_WhenLacksPermission_ShouldReturn403() throws Exception {
        // Arrange
        AuthorCreateRequestDTO request = new AuthorCreateRequestDTO();
        request.setName("Autor Prohibido");

        // Act & Assert
        mockMvc.perform(post("/api/authors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}