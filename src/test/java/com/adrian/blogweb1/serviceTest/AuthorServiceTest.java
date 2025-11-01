package com.adrian.blogweb1.serviceTest;

import com.adrian.blogweb1.dto.AuthorDTO;
import com.adrian.blogweb1.dto.AuthorCreateRequestDTO;
import com.adrian.blogweb1.dto.AuthorUpdateRequestDTO;
import com.adrian.blogweb1.model.Author;
import com.adrian.blogweb1.repository.IAuthorRepository;
import com.adrian.blogweb1.service.AuthorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.persistence.EntityNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorServiceTest {

    @Mock
    private IAuthorRepository authorRepository;

    @InjectMocks
    private AuthorService authorService;

    private Author author;

    // Este método se ejecuta antes de CADA test en esta clase.
    @BeforeEach
    void setUp() {
        // Preparamos un autor de prueba común que pueden usar múltiples tests
        author = new Author();
        author.setIdAuthor(1L);
        author.setName("Autor de Prueba");
    }

    @Test
    @DisplayName("Debería devolver una lista de todos los autores")
    void findAll_ShouldReturnListOfAuthors() {
        // --- 1. Arrange ---
        // Usamos el 'author' común del setUp y creamos uno adicional.
        Author author2 = new Author();
        author2.setIdAuthor(2L);
        author2.setName("Autor Dos");

        List<Author> listaDeAutores = List.of(author, author2);

        // b) Damos el guion al mock: "Cuando te llamen con findAll(), devuelve nuestra lista"
        when(authorRepository.findAll()).thenReturn(listaDeAutores);

        // --- 2. Act ---
        // Ejecutamos el método que queremos probar
        List<AuthorDTO> authors = authorService.getAuthorsDTO();

        // --- 3. Assert ---
        // Verificamos que el resultado es el esperado
        assertThat(authors).isNotNull();
        // Usando 'extracting', podemos verificar propiedades de la colección de forma más declarativa.
        assertThat(authors).hasSize(2)
                .extracting(AuthorDTO::getName) // Extrae el nombre de cada DTO en la lista
                .containsExactly("Autor de Prueba", "Autor Dos");
    }

    @Test
    @DisplayName("Debería devolver un autor por su ID cuando existe")
    void findById_WhenAuthorExists_ShouldReturnAuthor() {
        // --- 1. Arrange ---
        Long authorId = author.getIdAuthor();

        // b) Damos el guion al mock: "Cuando te llamen con findById(1L), devuelve un Optional con nuestro autor"
        when(authorRepository.findById(authorId)).thenReturn(Optional.of(this.author));

        // --- 2. Act ---
        // Ejecutamos el método que queremos probar
        Optional<AuthorDTO> foundAuthorOpt = authorService.getAuthorByIdDTO(authorId);

        // --- 3. Assert ---
        // Verificamos que el resultado es el esperado
        assertThat(foundAuthorOpt).isPresent(); // Verificamos que el Optional contiene un valor
        foundAuthorOpt.ifPresent(authorDTO -> {
            assertThat(authorDTO.getIdAuthor()).isEqualTo(this.author.getIdAuthor());
            assertThat(authorDTO.getName()).isEqualTo("Autor de Prueba");
        });
    }

    @Test
    @DisplayName("Debería devolver un Optional vacío cuando el autor no existe")
    void findById_WhenAuthorDoesNotExist_ShouldReturnEmptyOptional() {
        // --- 1. Arrange ---
        Long nonExistentId = 99L;
        when(authorRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // --- 2. Act ---
        Optional<AuthorDTO> result = authorService.getAuthorByIdDTO(nonExistentId);

        // --- 3. Assert ---
        assertThat(result).isNotPresent(); // O también se puede usar assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Debería crear un nuevo autor y devolver su DTO")
    void createAuthor_ShouldCreateAndReturnAuthorDTO() {
        // --- 1. Arrange ---
        // a) Datos de entrada para el método (lo que enviaría el cliente)
        AuthorCreateRequestDTO request = new AuthorCreateRequestDTO();
        request.setName("Nuevo Autor");

        // b) El objeto Author que el repositorio devolverá después de guardarlo (con ID)
        Author savedAuthor = new Author();
        savedAuthor.setIdAuthor(1L);
        savedAuthor.setName("Nuevo Autor");

        // c) Damos el guion al mock: "Cuando te llamen con save() con CUALQUIER objeto Author,
        //    devuelve nuestro 'savedAuthor' con ID". Usamos any() porque no podemos
        //    predecir la instancia exacta del objeto que se creará dentro del servicio.
        when(authorRepository.save(any(Author.class))).thenReturn(savedAuthor);

        // --- 2. Act ---
        // Ejecutamos el método que queremos probar
        AuthorDTO createdAuthorDTO = authorService.createAuthor(request);

        // --- 3. Assert ---
        // Verificamos que el resultado es el esperado
        assertThat(createdAuthorDTO).isNotNull();
        assertThat(createdAuthorDTO.getIdAuthor()).isEqualTo(1L);
        assertThat(createdAuthorDTO.getName()).isEqualTo("Nuevo Autor");

        // Opcional pero recomendado: verificar que el método save fue llamado exactamente una vez.
        verify(authorRepository).save(any(Author.class));
    }

    @Test
    @DisplayName("Debería actualizar un autor existente y devolver su DTO")
    void updateAuthor_WhenAuthorExists_ShouldUpdateAndReturnAuthorDTO() {
        // --- 1. Arrange ---
        // a) Datos de entrada
        Long existingId = 1L;
        AuthorUpdateRequestDTO request = new AuthorUpdateRequestDTO();
        request.setName("Nombre Actualizado");

        // c) Damos el guion a los mocks
        author.setName("Nombre Original"); // Ajustamos el nombre para este test
        when(authorRepository.findById(existingId)).thenReturn(Optional.of(author));
        when(authorRepository.save(any(Author.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // --- 2. Act ---
        AuthorDTO updatedAuthorDTO = authorService.updateAuthor(existingId, request);

        // --- 3. Assert ---
        assertThat(updatedAuthorDTO).isNotNull();
        assertThat(updatedAuthorDTO.getIdAuthor()).isEqualTo(existingId);
        assertThat(updatedAuthorDTO.getName()).isEqualTo("Nombre Actualizado");

        verify(authorRepository).findById(existingId);
        verify(authorRepository).save(any(Author.class));
    }

    @Test
    @DisplayName("Debería lanzar una excepción al intentar actualizar un autor que no existe")
    void updateAuthor_WhenAuthorDoesNotExist_ShouldThrowException() {
        // --- 1. Arrange ---
        Long nonExistentId = 99L;
        AuthorUpdateRequestDTO request = new AuthorUpdateRequestDTO();
        request.setName("Intento de actualización");

        when(authorRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // --- 2. Act & 3. Assert ---
        assertThatThrownBy(() -> authorService.updateAuthor(nonExistentId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Autor no encontrado con id: " + nonExistentId);

        verify(authorRepository, never()).save(any(Author.class));
    }

    @Test
    @DisplayName("Debería eliminar un autor cuando existe")
    void deleteAuthor_WhenAuthorExists_ShouldCompleteSuccessfully() {
        // --- 1. Arrange ---
        Long existingId = 1L;

        // Damos el guion: cuando se busque por el ID, se encontrará el autor.
        when(authorRepository.findById(existingId)).thenReturn(Optional.of(author));
        // No es estrictamente necesario mockear un método void, pero es buena práctica ser explícito.
        doNothing().when(authorRepository).delete(any(Author.class));

        // --- 2. Act ---
        // Ejecutamos el método. No esperamos que devuelva nada.
        authorService.deleteAuthor(existingId);

        // --- 3. Assert ---
        // Verificamos que se llamó al método delete del repositorio con el autor correcto.
        // Esto confirma que la lógica de búsqueda y eliminación se ejecutó.
        verify(authorRepository).findById(existingId);
        verify(authorRepository).delete(author);
    }

    @Test
    @DisplayName("Debería lanzar una excepción al intentar eliminar un autor que no existe")
    void deleteAuthor_WhenAuthorDoesNotExist_ShouldThrowException() {
        // --- 1. Arrange ---
        Long nonExistentId = 99L;
        // La forma más simple de evitar el error de "UnnecessaryStubbing" en este caso
        // es usar lenient(). Esto le dice a Mockito que esta simulación está bien
        // y que no debe considerarla "innecesaria" si la prueba termina en una excepción.
        lenient().when(authorRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // --- 2. Act & 3. Assert ---
        assertThatThrownBy(() -> authorService.deleteAuthor(nonExistentId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("No se puede eliminar. Autor no encontrado con id: " + nonExistentId);

        verify(authorRepository, never()).delete(any(Author.class));
    }
}