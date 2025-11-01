package com.adrian.blogweb1.repositoryTest;

import com.adrian.blogweb1.model.UserSec;
import com.adrian.blogweb1.repository.IUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

// 1. @DataJpaTest: La anotación clave. Configura un entorno de test solo para la capa JPA con H2.
@DataJpaTest
class UserRepositoryTest {

    // 2. @Autowired: Como @DataJpaTest levanta un contexto de Spring, podemos inyectar
    // la implementación REAL del repositorio que Spring crea para nosotros.
    @Autowired
    private IUserRepository userRepository;

    @Test
    @DisplayName("Debería encontrar un usuario por su username si existe")
    void findByUsername_WhenUserExists_ShouldReturnUser() {
        // --- 1. Arrange ---
        // Guardamos un usuario directamente en la base de datos de prueba (H2)
        // para tener datos con los que trabajar.
        UserSec userGuardado = new UserSec();
        userGuardado.setUsername("testuser");
        userGuardado.setPassword("password"); // Los campos no nulos deben tener valor
        userRepository.save(userGuardado);

        // --- 2. Act ---
        // Llamamos al método de la consulta personalizada que queremos probar.
        Optional<UserSec> resultado = userRepository.findByUsername("testuser");

        // --- 3. Assert ---
        // Verificamos que el repositorio encontró al usuario.
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Debería devolver un Optional vacío si el username no existe")
    void findByUsername_WhenUserDoesNotExist_ShouldReturnEmpty() {
        // --- 1. Arrange ---
        // No necesitamos guardar nada, porque la base de datos está vacía para este test.

        // --- 2. Act ---
        Optional<UserSec> resultado = userRepository.findByUsername("usuario_inexistente");

        // --- 3. Assert ---
        assertThat(resultado).isNotPresent();
    }
}
