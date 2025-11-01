package com.adrian.blogweb1.controllerTest;

import com.adrian.blogweb1.controller.PostController;
import com.adrian.blogweb1.dto.PostCreateRequestDTO;
import com.adrian.blogweb1.dto.PostUpdateRequestDTO;
import com.adrian.blogweb1.model.Post;
import com.adrian.blogweb1.dto.PostResponseDTO;
import com.adrian.blogweb1.service.IPostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


// Usamos @WebMvcTest para probar solo la capa web del PostController.
// Deshabilitamos los filtros de seguridad para que los tests sean rápidos.
@WebMvcTest(PostController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Mockeamos la dependencia del controlador.
    @MockBean
    private IPostService postService;

    @Test
    @DisplayName("GET /api/posts - Debería devolver una lista de posts")
    @WithMockUser
    void getAllPosts_ShouldReturnPostList() throws Exception {
        // Arrange: Preparamos la respuesta que el mock del servicio debe devolver.
        // Usamos el DTO correcto: PostResponseDTO. Asumimos que tiene un campo 'title'.
        PostResponseDTO post1 = new PostResponseDTO();
        post1.setTitle("Mi Primer Post");

        // Asumimos que tu servicio devuelve una lista de PostResponseDTO.
        when(postService.getPosts()).thenReturn(List.of(post1));

        // Act & Assert
        mockMvc.perform(get("/api/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Mi Primer Post"));
    }

    @Test
    @DisplayName("POST /api/posts - Debería crear un nuevo post y devolverlo")
    @WithMockUser
    void createPost_ShouldCreateAndReturnPost() throws Exception {
        // Arrange: Preparamos la petición y la respuesta esperada.
        // 1. La petición que enviaría el cliente (usando PostCreateRequestDTO)
        PostCreateRequestDTO request = new PostCreateRequestDTO();
        request.setTitle("Título del Nuevo Post");
        request.setContent("Contenido del post...");
        request.setAuthorId(1L); // <-- ¡AQUÍ ESTÁ LA SOLUCIÓN! Añadimos el ID del autor.

        // 2. La ENTIDAD que el servicio debería devolver después de guardar en la base de datos.
        Post savedPost = new Post();
        savedPost.setIdPost(1L);
        savedPost.setTitle("Título del Nuevo Post");
        savedPost.setContent("Contenido del post...");
        savedPost.setCreatedAt(LocalDateTime.now());

        // Configuramos el mock para que devuelva la entidad Post, que es lo que el método espera.
        when(postService.savePost(any(PostCreateRequestDTO.class))).thenReturn(savedPost);

        // Act & Assert
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Título del Nuevo Post"));
    }

    @Test
    @DisplayName("GET /api/posts/{id} - Debería devolver un post por su ID")
    @WithMockUser
    void getPostById_WhenPostExists_ShouldReturnPost() throws Exception {
        // Arrange
        long postId = 1L;
        PostResponseDTO postResponse = new PostResponseDTO();
        postResponse.setIdPost(postId);
        postResponse.setTitle("Post Individual");

        // Asumimos que tu servicio devuelve un Optional<PostResponseDTO>
        when(postService.getPostById(postId)).thenReturn(Optional.of(postResponse));

        // Act & Assert
        mockMvc.perform(get("/api/posts/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPost").value(1L))
                .andExpect(jsonPath("$.title").value("Post Individual"));
    }

    @Test
    @DisplayName("GET /api/posts/{id} - Debería devolver 404 si el post no existe")
    @WithMockUser
    void getPostById_WhenPostDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Arrange
        long nonExistentId = 99L;
        when(postService.getPostById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/posts/{id}", nonExistentId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PUT /api/posts/{id} - Debería actualizar un post existente")
    @WithMockUser
    void updatePost_WhenPostExists_ShouldUpdateAndReturnPost() throws Exception {
        // Arrange
        long postId = 1L;
        PostUpdateRequestDTO updateRequest = new PostUpdateRequestDTO();
        updateRequest.setTitle("Título Actualizado");
        updateRequest.setContent("Contenido actualizado...");

        PostResponseDTO updatedResponse = new PostResponseDTO();
        updatedResponse.setIdPost(postId);
        updatedResponse.setTitle("Título Actualizado");

        // Configuramos el mock para que devuelva el DTO actualizado cuando se llame al método update
        when(postService.updatePost(eq(postId), any(PostUpdateRequestDTO.class))).thenReturn(updatedResponse);

        // Act & Assert
        mockMvc.perform(put("/api/posts/{id}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPost").value(postId))
                .andExpect(jsonPath("$.title").value("Título Actualizado"));
    }

    @Test
    @DisplayName("PUT /api/posts/{id} - Debería devolver 404 si el post a actualizar no existe")
    @WithMockUser
    void updatePost_WhenPostDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Arrange
        long nonExistentId = 99L;
        PostUpdateRequestDTO updateRequest = new PostUpdateRequestDTO();
        updateRequest.setTitle("Título Fallido");
        // ¡AQUÍ ESTÁ LA SOLUCIÓN! Añadimos el contenido para que la petición sea válida.
        updateRequest.setContent("Contenido válido para pasar la validación");
        // Configuramos el mock para que lance la excepción cuando se intente actualizar un post no existente
        when(postService.updatePost(eq(nonExistentId), any(PostUpdateRequestDTO.class))).thenThrow(new jakarta.persistence.EntityNotFoundException());

        // Act & Assert
        mockMvc.perform(put("/api/posts/{id}", nonExistentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/posts/{id} - Debería eliminar un post existente y devolver 204 No Content")
    @WithMockUser
    void deletePost_WhenPostExists_ShouldReturnNoContent() throws Exception {
        // Arrange
        long postId = 1L;
        // Configuramos el mock para que no haga nada cuando se llame a deletePost,
        // ya que es un método void.
        doNothing().when(postService).deletePost(postId);

        // Act & Assert
        mockMvc.perform(delete("/api/posts/{id}", postId))
                .andExpect(status().isNoContent()); // Espera un estado HTTP 204 No Content
    }

    @Test
    @DisplayName("DELETE /api/posts/{id} - Debería devolver 404 si el post a eliminar no existe")
    @WithMockUser
    void deletePost_WhenPostDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Arrange
        long nonExistentId = 99L;
        // Configuramos el mock para que lance la excepción esperada del servicio.
        // Usamos org.mockito.Mockito.doThrow para métodos void.
        org.mockito.Mockito.doThrow(new jakarta.persistence.EntityNotFoundException())
                .when(postService).deletePost(nonExistentId);

        // Act & Assert
        mockMvc.perform(delete("/api/posts/{id}", nonExistentId))
                .andExpect(status().isNotFound()); // Espera un estado HTTP 404 Not Found
    }

}
