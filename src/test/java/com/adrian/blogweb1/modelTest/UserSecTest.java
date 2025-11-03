package com.adrian.blogweb1.modelTest;


import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.model.UserSec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserSecTest {

    @Test
    @DisplayName("getAuthorities debería devolver las autoridades correctas basadas en los roles")
    void getAuthorities_shouldReturnCorrectAuthoritiesFromRoles() {
        // --- 1. Arrange ---
        // Creamos los roles
        Role userRole = new Role();
        userRole.setRole("USER");

        Role adminRole = new Role();
        adminRole.setRole("ADMIN");

        // Creamos el usuario y le asignamos los roles
        UserSec user = new UserSec();
        user.setRolesList(Set.of(userRole, adminRole));

        // --- 2. Act ---
        // Llamamos al método que queremos probar
        Collection<GrantedAuthority> authorities = user.getAuthorities();

        // --- 3. Assert ---
        // Verificamos que la colección de autoridades es la esperada
        assertThat(authorities).isNotNull();
        assertThat(authorities).hasSize(2);
        assertThat(authorities).containsExactlyInAnyOrder(
                // --- INICIO DE LA SOLUCIÓN ---
                // Corregimos la aserción para que coincida con el prefijo "ROLE_" que se espera.
                new SimpleGrantedAuthority("ROLE_USER"),
                new SimpleGrantedAuthority("ROLE_ADMIN")
        );
    }

    @Test
    @DisplayName("getAuthorities debería devolver una colección vacía si no hay roles")
    void getAuthorities_whenNoRoles_shouldReturnEmptyCollection() {
        // --- 1. Arrange ---
        UserSec user = new UserSec();
        // No le asignamos roles, el Set está vacío por defecto

        // --- 2. Act ---
        Collection<GrantedAuthority> authorities = user.getAuthorities();

        // --- 3. Assert ---
        assertThat(authorities).isNotNull();
        assertThat(authorities).isEmpty();
    }
}
