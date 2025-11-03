package com.adrian.blogweb1.serviceTest;

import com.adrian.blogweb1.model.Permission;
import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.model.UserSec;
import com.adrian.blogweb1.repository.IUserRepository;
import com.adrian.blogweb1.service.UserDetailsServiceImp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImpTest {

    @Mock
    private IUserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImp userDetailsService;

    @Test
    @DisplayName("Debería cargar UserDetails con roles y permisos cuando el usuario existe")
    void loadUserByUsername_whenUserExists_shouldReturnUserDetails() {
        // --- 1. Arrange ---
        String username = "testuser";

        // Creamos los permisos
        Permission readPermission = new Permission("READ");
        Permission writePermission = new Permission("WRITE");

        // Creamos el rol y le asignamos los permisos
        Role userRole = new Role();
        userRole.setRole("USER");
        userRole.setPermissionsList(Set.of(readPermission, writePermission));

        // Creamos el usuario de la base de datos
        UserSec userFromDb = new UserSec();
        userFromDb.setUsername(username);
        userFromDb.setPassword("password123");
        userFromDb.setEnabled(true);
        userFromDb.setAccountNotExpired(true);
        userFromDb.setCredentialNotExpired(true);
        userFromDb.setAccountNotLocked(true);
        userFromDb.setRolesList(Set.of(userRole));

        // Damos el guion al mock del repositorio
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(userFromDb));

        // --- 2. Act ---
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // --- 3. Assert ---
        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo(username);
        assertThat(userDetails.getPassword()).isEqualTo("password123");
        assertThat(userDetails.isEnabled()).isTrue();

        // Verificamos que las autoridades (roles y permisos) se cargaron correctamente
        assertThat(userDetails.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "READ", "WRITE");
    }

    @Test
    @DisplayName("Debería lanzar UsernameNotFoundException si el usuario no existe")
    void loadUserByUsername_whenUserDoesNotExist_shouldThrowException() {
        // --- 1. Arrange ---
        String username = "nonexistent";

        // Damos el guion al mock: cuando se busque este usuario, no se encontrará nada.
        when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

        // --- 2. Act & 3. Assert ---
        // Verificamos que se lanza la excepción correcta y que el mensaje es el esperado.
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername(username);
        });

        assertThat(exception.getMessage()).isEqualTo("Usuario no encontrado con el nombre: " + username);
    }
}
