package com.adrian.blogweb1.dtoTest;


import com.adrian.blogweb1.dto.PostResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class postResponseDTOTest {

    @Test
    @DisplayName("Debería crear un DTO y verificar getters y setters")
    void testGettersAndSetters() {
        // --- 1. Arrange ---
        // Creamos una instancia vacía del DTO
        PostResponseDTO dto = new PostResponseDTO();
        LocalDateTime now = LocalDateTime.now();

        // --- 2. Act ---
        // Usamos todos los setters para asignar valores
        dto.setIdPost(1L);
        dto.setTitle("Título de Prueba");
        dto.setContent("Contenido de prueba.");
        dto.setCreatedAt(now);
        dto.setAuthorName("Autor de Prueba");

        // --- 3. Assert ---
        // Verificamos que cada getter devuelve el valor que asignamos
        assertThat(dto.getIdPost()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("Título de Prueba");
        assertThat(dto.getContent()).isEqualTo("Contenido de prueba.");
        assertThat(dto.getCreatedAt()).isEqualTo(now);
        assertThat(dto.getAuthorName()).isEqualTo("Autor de Prueba");
    }

    @Test
    @DisplayName("Debería crear un DTO usando el constructor con todos los argumentos")
    void testAllArgsConstructor() {
        // --- 1. Arrange ---
        LocalDateTime now = LocalDateTime.now();

        // --- 2. Act ---
        // Creamos una instancia usando el constructor que recibe todos los parámetros
        PostResponseDTO dto = new PostResponseDTO(
                2L,
                "Otro Título",
                "Otro contenido.",
                now,
                "Otro Autor"
        );

        // --- 3. Assert ---
        // Verificamos que los campos se inicializaron correctamente
        assertThat(dto.getIdPost()).isEqualTo(2L);
        assertThat(dto.getTitle()).isEqualTo("Otro Título");
        assertThat(dto.getContent()).isEqualTo("Otro contenido.");
        assertThat(dto.getCreatedAt()).isEqualTo(now);
        assertThat(dto.getAuthorName()).isEqualTo("Otro Autor");
    }
}
