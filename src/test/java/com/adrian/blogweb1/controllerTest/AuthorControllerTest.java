package com.adrian.blogweb1.controllerTest;

import com.adrian.blogweb1.controller.AuthorController;
import com.adrian.blogweb1.dto.AuthorCreateRequestDTO;
import com.adrian.blogweb1.dto.AuthorDTO;
import com.adrian.blogweb1.dto.AuthorUpdateRequestDTO;
import com.adrian.blogweb1.service.IAuthorService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 1. Usamos @WebMvcTest para probar solo la capa web. Es rápido y ligero.
@WebMvcTest(AuthorController.class)
class AuthorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // 3. Mockeamos la dependencia del controlador. @MockBean es la forma correcta aquí.
    @MockBean
    private IAuthorService authorService;

    @Test
    @DisplayName("GET /api/authors - Debería devolver una lista de autores")
    // 4. Usamos @WithMockUser para simular un usuario y evitar errores de seguridad básicos.
    @WithMockUser(authorities = "READ")
    void getAllAuthors_ShouldReturnAuthorList() throws Exception {
        // Arrange
        AuthorDTO author1 = new AuthorDTO(1L, "Autor 1");
        AuthorDTO author2 = new AuthorDTO(2L, "Autor 2");
        when(authorService.getAuthorsDTO()).thenReturn(List.of(author1, author2));

        // Act & Assert
        mockMvc.perform(get("/api/authors"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Autor 1")));
    }

    @Test
    @DisplayName("GET /api/authors/{id} - Debería devolver un autor si existe")
    @WithMockUser(authorities = "READ")
    void getAuthorById_WhenAuthorExists_ShouldReturnAuthor() throws Exception {
        // Arrange
        AuthorDTO author = new AuthorDTO(1L, "Autor de Prueba");
        when(authorService.getAuthorByIdDTO(1L)).thenReturn(Optional.of(author));

        // Act & Assert
        mockMvc.perform(get("/api/authors/1"))
                .andExpect(status().isOk())
                // --- INICIO DE LA SOLUCIÓN ---
                // Corregimos el JSON path para que coincida con el campo "idAuthor" del DTO.
                .andExpect(jsonPath("$.idAuthor", is(1)))
                .andExpect(jsonPath("$.name", is("Autor de Prueba")));
    }

    @Test
    @DisplayName("GET /api/authors/{id} - Debería devolver 404 si el autor no existe")
    @WithMockUser(authorities = "READ")
    void getAuthorById_WhenAuthorDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(authorService.getAuthorByIdDTO(99L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/authors/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/authors - Debería crear un autor y devolver 201 Created")
    @WithMockUser(authorities = "CREATE")
    void createAuthor_ShouldReturnCreated() throws Exception {
        // Arrange
        // --- INICIO DE LA SOLUCIÓN ---
        AuthorCreateRequestDTO request = new AuthorCreateRequestDTO();
        request.setName("Nuevo Autor");
        AuthorDTO createdAuthor = new AuthorDTO(1L, "Nuevo Autor");

        when(authorService.createAuthor(any(AuthorCreateRequestDTO.class))).thenReturn(createdAuthor);

        // Act & Assert
        mockMvc.perform(post("/api/authors")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idAuthor", is(1)))
                .andExpect(jsonPath("$.name", is("Nuevo Autor")));
    }

    @Test
    @DisplayName("PUT /api/authors/{id} - Debería actualizar un autor y devolver 200 OK")
    @WithMockUser(authorities = "UPDATE")
    void updateAuthor_ShouldReturnOk() throws Exception {
        // Arrange
        long authorId = 1L;
        AuthorUpdateRequestDTO request = new AuthorUpdateRequestDTO();
        request.setName("Autor Actualizado");
        AuthorDTO updatedAuthor = new AuthorDTO(authorId, "Autor Actualizado");

        when(authorService.updateAuthor(eq(authorId), any(AuthorUpdateRequestDTO.class))).thenReturn(updatedAuthor);

        // Act & Assert
        mockMvc.perform(put("/api/authors/{id}", authorId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Autor Actualizado")));
    }

    @Test
    @DisplayName("PUT /api/authors/{id} - Debería devolver 404 si el autor a actualizar no existe")
    @WithMockUser(authorities = "UPDATE")
    void updateAuthor_WhenAuthorDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Arrange
        long nonExistentId = 99L;
        AuthorUpdateRequestDTO request = new AuthorUpdateRequestDTO();
        request.setName("No importa");

        // Simulamos que el servicio lanza la excepción que el @ExceptionHandler debe capturar
        when(authorService.updateAuthor(eq(nonExistentId), any(AuthorUpdateRequestDTO.class)))
                .thenThrow(new EntityNotFoundException("Autor no encontrado con ID: " + nonExistentId));

        // Act & Assert
        mockMvc.perform(put("/api/authors/{id}", nonExistentId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Autor no encontrado con ID: 99"));
    }

    @Test
    @DisplayName("DELETE /api/authors/{id} - Debería eliminar un autor y devolver 204 No Content")
    @WithMockUser(authorities = "DELETE")
    void deleteAuthor_ShouldReturnNoContent() throws Exception {
        // Arrange
        long authorId = 1L;
        // No es necesario un 'when' para métodos void, Mockito no hará nada por defecto.

        // Act & Assert
        mockMvc.perform(delete("/api/authors/{id}", authorId)
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }
}