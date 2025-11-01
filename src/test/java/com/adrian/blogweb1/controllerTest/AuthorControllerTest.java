package com.adrian.blogweb1.controllerTest;

import com.adrian.blogweb1.controller.AuthorController;
import com.adrian.blogweb1.dto.AuthorDTO;
import com.adrian.blogweb1.service.IAuthorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 1. Usamos @WebMvcTest para probar solo la capa web. Es rápido y ligero.
// 2. NO importamos ninguna configuración de seguridad.
@WebMvcTest(AuthorController.class)
class AuthorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // 3. Mockeamos la dependencia del controlador. @MockBean es la forma correcta aquí.
    //    La advertencia de "deprecated" NO aparece en este contexto.
    @MockBean
    private IAuthorService authorService;

    @Test
    @DisplayName("GET /api/authors - Debería devolver una lista de autores")
    // 4. Usamos @WithMockUser para simular un usuario y evitar errores de seguridad básicos.
    @WithMockUser
    void getAllAuthors_ShouldReturnAuthorList() throws Exception {
        // Arrange
        when(authorService.getAuthorsDTO()).thenReturn(List.of(new AuthorDTO(1L, "Autor 1")));

        // Act & Assert
        mockMvc.perform(get("/api/authors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Autor 1"));
    }

    // ... Aquí irían el resto de tus pruebas de LÓGICA (POST, PUT, DELETE, etc.)
    //     siempre usando @WithMockUser para que la seguridad no moleste.
}