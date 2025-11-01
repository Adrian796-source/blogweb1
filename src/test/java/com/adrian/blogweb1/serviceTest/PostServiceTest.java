package com.adrian.blogweb1.serviceTest;



import com.adrian.blogweb1.dto.PostCreateRequestDTO;
import com.adrian.blogweb1.dto.PostResponseDTO;
import com.adrian.blogweb1.dto.PostUpdateRequestDTO;
import com.adrian.blogweb1.model.Author;
import com.adrian.blogweb1.model.Post;
import com.adrian.blogweb1.repository.IAuthorRepository;
import com.adrian.blogweb1.repository.IPostRepository;
import com.adrian.blogweb1.service.PostService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Mock
    private IPostRepository postRepository;

    @Mock
    private IAuthorRepository authorRepository;

    @InjectMocks
    private PostService postService;

    private Author author;
    private Post post;

    // Este método se ejecuta antes de CADA test en esta clase.
    @BeforeEach
    void setUp() {
        // Preparamos un autor y un post de prueba comunes
        author = new Author();
        author.setIdAuthor(1L);
        author.setName("Autor de Prueba");

        post = new Post();
        post.setIdPost(101L);
        post.setTitle("Título de Prueba");
        post.setContent("Contenido de Prueba");
        post.setAuthor(author);
    }

    @Test
    @DisplayName("Debería guardar un post cuando el autor existe")
    void savePost_WhenAuthorExists_ShouldSavePost() {
        // --- 1. Arrange ---
        // a) Creamos el DTO que simula la petición de entrada
        PostCreateRequestDTO request = new PostCreateRequestDTO();
        request.setTitle("Nuevo Post de Prueba");
        request.setContent("Contenido del post de prueba.");
        request.setAuthorId(1L);

        // c) Damos el guion a nuestros mocks
        // Cuando el authorRepository busque el ID 1, lo encontrará
        when(authorRepository.findById(1L)).thenReturn(Optional.of(author));
        // Cuando el postRepository guarde cualquier Post, simplemente lo devolverá
        when(postRepository.save(any(Post.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // d) Creamos un "capturador" para verificar qué objeto se pasa al método save
        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);

        // --- 2. Act ---
        // Ejecutamos el método que queremos probar
        postService.savePost(request);

        // --- 3. Assert ---
        // Verificamos que el método save del postRepository fue llamado exactamente 1 vez
        verify(postRepository).save(postCaptor.capture());
        Post postGuardado = postCaptor.getValue();

        // Verificamos que los datos en el objeto Post que se guardó son los correctos
        assertThat(postGuardado.getTitle()).isEqualTo("Nuevo Post de Prueba");
        assertThat(postGuardado.getAuthor().getName()).isEqualTo("Autor de Prueba");
    }

    @Test
    @DisplayName("Debería lanzar una excepción al intentar guardar un post con un autor que no existe")
    void savePost_WhenAuthorDoesNotExist_ShouldThrowException() {
        // --- 1. Arrange ---
        // a) Creamos el DTO con un ID de autor que no existirá
        PostCreateRequestDTO request = new PostCreateRequestDTO();
        request.setAuthorId(999L); // ID inexistente
        request.setTitle("Título de prueba");
        request.setContent("Contenido de prueba");

        // b) Damos el guion al mock: "Cuando busquen el autor con ID 999, devuelve un Optional vacío"
        when(authorRepository.findById(999L)).thenReturn(Optional.empty());

        // --- 2. Act & 3. Assert ---
        // Verificamos que se lanza la excepción RuntimeException cuando llamamos al método.
        assertThrows(RuntimeException.class, () -> {
            postService.savePost(request);
        });

        // Verificación extra: Nos aseguramos de que NUNCA se llamó al método save del postRepository,
        // porque la ejecución debería haberse detenido antes.
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("Debería devolver una lista de DTOs de posts cuando existen posts")
    void getPosts_WhenPostsExist_ShouldReturnPostResponseDTOList() {
        // --- 1. Arrange ---
        Post post2 = new Post();
        post2.setIdPost(102L);
        post2.setTitle("Título 2");
        post2.setAuthor(author);
        List<Post> listaDePosts = List.of(post, post2);

        // b) Damos el guion al mock: "Cuando te llamen con findAll(), devuelve nuestra lista de posts"
        when(postRepository.findAll()).thenReturn(listaDePosts);

        // --- 2. Act ---
        // Ejecutamos el método que queremos probar
        List<PostResponseDTO> resultado = postService.getPosts();

        // --- 3. Assert ---
        // Verificamos que el resultado es el esperado
        assertThat(resultado).isNotNull();
        assertThat(resultado.size()).isEqualTo(2); // Debería haber 2 DTOs
        assertThat(resultado.get(0).getTitle()).isEqualTo("Título de Prueba"); // Verificamos el título del primer DTO
        assertThat(resultado.get(1).getAuthorName()).isEqualTo("Autor de Prueba"); // Verificamos el autor del segundo DTO
    }

    @Test
    @DisplayName("Debería devolver una lista vacía cuando no existen posts")
    void getPosts_WhenNoPostsExist_ShouldReturnEmptyList() {
        // --- 1. Arrange ---
        // Damos el guion al mock: "Cuando te llamen con findAll(), devuelve una lista vacía"
        when(postRepository.findAll()).thenReturn(Collections.emptyList());

        // --- 2. Act ---
        List<PostResponseDTO> resultado = postService.getPosts();

        // --- 3. Assert ---
        // Verificamos que el resultado es una lista no nula pero vacía
        assertThat(resultado).isNotNull();
        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("Debería devolver un Optional con un DTO de post cuando el ID existe")
    void getPostById_WhenPostExists_ShouldReturnOptionalOfPostResponseDTO() {
        // --- 1. Arrange ---
        // b) Damos el guion al mock: "Cuando te pidan el post con ID 1, devuelve este post"
        when(postRepository.findById(post.getIdPost())).thenReturn(Optional.of(post));

        // --- 2. Act ---
        // Ejecutamos el método que queremos probar
        Optional<PostResponseDTO> resultado = postService.getPostById(post.getIdPost());

        // --- 3. Assert ---
        // Verificamos que el Optional contiene un valor
        assertThat(resultado).isPresent();
        // Verificamos que los datos dentro del DTO son los correctos
        assertThat(resultado.get().getTitle()).isEqualTo("Título de Prueba");
        assertThat(resultado.get().getAuthorName()).isEqualTo("Autor de Prueba");
    }

    @Test
    @DisplayName("Debería actualizar un post y devolver su DTO cuando el post existe")
    void updatePost_WhenPostExists_ShouldUpdateAndReturnDTO() {
        // --- 1. Arrange ---
        // a) Creamos los datos de prueba
        long postId = 1L;
        Author autor = new Author();
        autor.setName("Autor Original");

        // El post que simularemos que ya existe en la BD
        Post postExistente = new Post();
        postExistente.setIdPost(postId);
        postExistente.setTitle("Título Antiguo");
        postExistente.setContent("Contenido Antiguo");
        postExistente.setAuthor(autor);

        // El DTO con los nuevos detalles que vienen en la petición
        PostUpdateRequestDTO detallesNuevos = new PostUpdateRequestDTO();
        detallesNuevos.setTitle("Título Nuevo");
        detallesNuevos.setContent("Contenido Nuevo");

        // b) Damos el guion a los mocks
        // Cuando se busque el post por ID, se encontrará
        when(postRepository.findById(postId)).thenReturn(Optional.of(postExistente));
        // Cuando se guarde el post actualizado, simplemente lo devolverá
        when(postRepository.save(any(Post.class))).thenAnswer(inv -> inv.getArgument(0));

        // --- 2. Act ---
        // Ejecutamos el método que queremos probar
        PostResponseDTO resultadoDTO = postService.updatePost(postId, detallesNuevos);

        // --- 3. Assert ---
        // Verificamos que el DTO devuelto contiene los datos actualizados
        assertThat(resultadoDTO).isNotNull();
        assertThat(resultadoDTO.getTitle()).isEqualTo("Título Nuevo");
        assertThat(resultadoDTO.getContent()).isEqualTo("Contenido Nuevo");
        // Verificamos que los datos no modificados (como el autor) siguen ahí
        assertThat(resultadoDTO.getAuthorName()).isEqualTo("Autor Original");

        // Verificación extra con ArgumentCaptor para asegurar qué se guardó
        ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
        verify(postRepository).save(postCaptor.capture());
        Post postGuardado = postCaptor.getValue();

        // Verificamos que el objeto que se pasó al método save() tenía los datos correctos
        assertThat(postGuardado.getTitle()).isEqualTo("Título Nuevo");
    }

    @Test
    @DisplayName("Debería lanzar EntityNotFoundException al intentar actualizar un post que no existe")
    void updatePost_WhenPostDoesNotExist_ShouldThrowEntityNotFoundException() {
        // --- 1. Arrange ---
        long postIdQueNoExiste = 999L;
        PostUpdateRequestDTO detallesNuevos = new PostUpdateRequestDTO();
        detallesNuevos.setTitle("No importa");

        // Damos el guion al mock: "Cuando busquen el post con ID 999, devuelve un Optional vacío"
        when(postRepository.findById(postIdQueNoExiste)).thenReturn(Optional.empty());

        // --- 2. Act & 3. Assert ---
        // Verificamos que se lanza la excepción correcta cuando llamamos al método.
        assertThrows(EntityNotFoundException.class, () -> {
            postService.updatePost(postIdQueNoExiste, detallesNuevos);
        });

        // Verificación extra: Nos aseguramos de que NUNCA se llamó al método save,
        // porque la ejecución debería haberse detenido antes.
        verify(postRepository, never()).save(any(Post.class));
    }

    @Test
    @DisplayName("Debería llamar al método deleteById del repositorio cuando el post existe")
    void deletePost_WhenPostExists_ShouldCallRepositoryDeleteById() {
        // --- 1. Arrange ---
        long postId = 1L;

        // Guion: Cuando se verifique si el post existe, se encontrará.
        when(postRepository.existsById(postId)).thenReturn(true);

        // --- 2. Act ---
        // Ejecutamos el método que queremos probar.
        postService.deletePost(postId);

        // --- 3. Assert ---
        // Verificamos que el método 'deleteById' de nuestro mock del repositorio
        // fue llamado exactamente 1 vez con el 'postId' correcto como argumento.
        verify(postRepository, times(1)).deleteById(postId);
    }

    @Test
    @DisplayName("Debería lanzar EntityNotFoundException al intentar borrar un post que no existe")
    void deletePost_WhenPostDoesNotExist_ShouldThrowEntityNotFoundException() {
        // --- 1. Arrange ---
        long postIdQueNoExiste = 999L;

        // Guion: Cuando se verifique si el post existe, no se encontrará.
        when(postRepository.existsById(postIdQueNoExiste)).thenReturn(false);

        // --- 2. Act & 3. Assert ---
        assertThrows(EntityNotFoundException.class, () -> {
            postService.deletePost(postIdQueNoExiste);
        });

        verify(postRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Debería devolver un Optional vacío cuando el ID del post no existe")
    void getPostById_WhenPostDoesNotExist_ShouldReturnEmptyOptional() {
        // --- 1. Arrange ---
        long postIdQueNoExiste = 999L;

        // Damos el guion al mock: "Cuando te pidan el post con ID 999, devuelve un Optional vacío"
        when(postRepository.findById(postIdQueNoExiste)).thenReturn(Optional.empty());

        // --- 2. Act ---
        Optional<PostResponseDTO> resultado = postService.getPostById(postIdQueNoExiste);

        // --- 3. Assert ---
        // Verificamos que el Optional está vacío
        assertThat(resultado).isNotPresent();
    }




}
