package com.adrian.blogweb1.integrationTest;

import com.adrian.blogweb1.dto.PostCreateRequestDTO;
import com.adrian.blogweb1.dto.PostUpdateRequestDTO;
import com.adrian.blogweb1.model.Author;
import com.adrian.blogweb1.model.Post;
import com.adrian.blogweb1.repository.IAuthorRepository;
import com.adrian.blogweb1.repository.IPostRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=password",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
public class PostControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IPostRepository postRepository;

    @Autowired
    private IAuthorRepository authorRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        authorRepository.deleteAll();
    }

    // --- TESTS DE LECTURA (GET) ---

    @Test
    @DisplayName("GET /api/posts debería devolver una lista de posts")
    @WithMockUser(authorities = "READ")
    void getPosts_ShouldReturnListOfPosts() throws Exception {
        // Arrange
        Author author = new Author();
        author.setName("Autor de Prueba");
        authorRepository.save(author);

        Post post1 = new Post();
        post1.setTitle("Título 1");
        post1.setContent("Contenido 1");
        post1.setAuthor(author);
        postRepository.save(post1);

        Post post2 = new Post();
        post2.setTitle("Título 2");
        post2.setContent("Contenido 2");
        post2.setAuthor(author);
        postRepository.save(post2);

        // Act & Assert
        mockMvc.perform(get("/api/posts").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Título 1")));
    }

    @Test
    @DisplayName("GET /api/posts/{id} debería devolver un post cuando el ID existe")
    @WithMockUser(authorities = "READ")
    void getPostById_WhenPostExists_ShouldReturnPost() throws Exception {
        // Arrange
        Author author = new Author();
        author.setName("Autor de Prueba");
        authorRepository.save(author);

        Post post = new Post();
        post.setTitle("Título de Prueba");
        post.setContent("Contenido");
        post.setAuthor(author);
        Post postGuardado = postRepository.save(post);
        Long idReal = postGuardado.getIdPost();

        // Act & Assert
        mockMvc.perform(get("/api/posts/" + idReal).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Título de Prueba")));
    }

    @Test
    @DisplayName("GET /api/posts/{id} debería devolver 404 Not Found cuando el ID no existe")
    @WithMockUser(authorities = "READ")
    void getPostById_WhenPostDoesNotExist_ShouldReturnNotFound() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/posts/999").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // --- TEST DE CREACIÓN (POST) ---

    @Test
    @DisplayName("POST /api/posts debería crear un nuevo post")
    @WithMockUser(authorities = "CREATE")
    void createPost_ShouldCreateNewPost() throws Exception {
        // Arrange
        Author author = new Author();
        author.setName("Autor para Post");
        Author authorGuardado = authorRepository.save(author);

        PostCreateRequestDTO postRequest = new PostCreateRequestDTO();
        postRequest.setTitle("Nuevo Post desde IT");
        postRequest.setContent("Contenido del nuevo post.");
        postRequest.setAuthorId(authorGuardado.getIdAuthor());

        // Act & Assert
        mockMvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Nuevo Post desde IT")));
    }

    // --- TEST DE ACTUALIZACIÓN (PUT) ---

    @Test
    @DisplayName("PUT /api/posts/{id} debería actualizar un post existente")
    @WithMockUser(authorities = "UPDATE")
    void updatePost_ShouldUpdatePost() throws Exception {
        // Arrange
        Author author = new Author();
        author.setName("Autor Original");
        authorRepository.save(author);

        Post postExistente = new Post();
        postExistente.setTitle("Título Antiguo");
        postExistente.setContent("Contenido Antiguo");
        postExistente.setAuthor(author);
        Post postGuardado = postRepository.save(postExistente);
        Long idReal = postGuardado.getIdPost();

        PostUpdateRequestDTO updateRequest = new PostUpdateRequestDTO();
        updateRequest.setTitle("Título Actualizado");
        updateRequest.setContent("Contenido Actualizado");

        // Act & Assert
        mockMvc.perform(put("/api/posts/" + idReal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Título Actualizado")));
    }

    // --- TEST DE BORRADO (DELETE) ---

    @Test
    @DisplayName("DELETE /api/posts/{id} debería eliminar un post")
    @WithMockUser(authorities = "DELETE")
    void deletePost_ShouldDeletePost() throws Exception {
        // Arrange
        Author author = new Author();
        author.setName("Autor a Borrar");
        authorRepository.save(author);

        Post postExistente = new Post();
        postExistente.setTitle("Post a Borrar");
        postExistente.setContent("Contenido");
        postExistente.setAuthor(author);
        Post postGuardado = postRepository.save(postExistente);
        Long idReal = postGuardado.getIdPost();

        // Act & Assert
        mockMvc.perform(delete("/api/posts/" + idReal))
                .andExpect(status().isOk());

        // Assert final: verificar que el post ya no está en la BD
        assertThat(postRepository.findById(idReal)).isEmpty();
    }
}