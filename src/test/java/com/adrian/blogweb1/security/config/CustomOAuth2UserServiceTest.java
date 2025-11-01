package com.adrian.blogweb1.security.config;

import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.model.UserSec;
import com.adrian.blogweb1.repository.IRoleRepository;
import com.adrian.blogweb1.repository.IUserRepository;
import com.adrian.blogweb1.security.config.props.DefaultAdminProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock
    private IUserRepository userRepository;

    @Mock
    private IRoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DefaultAdminProperties adminProperties;

    @Mock
    private OAuth2User oauthUser;

    // Usamos @InjectMocks, que es más simple y directo.
    @InjectMocks
    private CustomOAuth2UserService customOAuth2UserService;

    private Role userRole;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        userRole = new Role();
        userRole.setIdRole(1L);
        userRole.setRole("ROLE_USER");

        adminRole = new Role();
        adminRole.setIdRole(2L);
        adminRole.setRole("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Debería encontrar y devolver un usuario existente")
    void findOrCreateUser_WhenUserExists_ShouldReturnExistingUser() {
        // --- 1. Arrange ---
        String email = "test@example.com";
        String username = "testuser";

        // Simulamos la información que viene de GitHub
        when(oauthUser.getAttribute("email")).thenReturn(email);
        when(oauthUser.getAttribute("login")).thenReturn(username);

        // Simulamos que el usuario ya existe en nuestra BD
        UserSec existingUser = new UserSec();
        existingUser.setEmail(email);
        existingUser.setUsername("old_username");
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));

        // Simulamos la búsqueda del rol
        when(roleRepository.findByRole("ROLE_USER")).thenReturn(Optional.of(userRole));

        // Simulamos el guardado del usuario y CAPTURAMOS el objeto
        ArgumentCaptor<UserSec> userCaptor = ArgumentCaptor.forClass(UserSec.class);
        when(userRepository.save(userCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        // --- 2. Act ---
        // Llamamos directamente al método con la lógica de negocio.
        UserSec resultUser = customOAuth2UserService.findOrCreateUser(oauthUser);

        // --- 3. Assert ---
        assertThat(resultUser).isNotNull();
        // Verificamos que el UserSec guardado tenga el username actualizado
        assertThat(userCaptor.getValue().getUsername()).isEqualTo(username);
    }

    @Test
    @DisplayName("Debería crear un nuevo usuario si no existe")
    void findOrCreateUser_WhenUserDoesNotExist_ShouldCreateNewUser() {
        // --- 1. Arrange ---
        String email = "newuser@example.com";
        String username = "newuser";

        when(oauthUser.getAttribute("email")).thenReturn(email);
        when(oauthUser.getAttribute("login")).thenReturn(username);

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(roleRepository.findByRole("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(UserSec.class))).thenAnswer(invocation -> {
            UserSec userToSave = invocation.getArgument(0);
            userToSave.setIdUserSec(1L);
            return userToSave;
        });

        // --- 2. Act ---
        UserSec resultUser = customOAuth2UserService.findOrCreateUser(oauthUser);

        // --- 3. Assert ---
        assertThat(resultUser).isNotNull();
        assertThat(resultUser.getIdUserSec()).isNotNull();
        assertThat(resultUser.getEmail()).isEqualTo(email);
        assertThat(resultUser.getUsername()).isEqualTo(username);
        assertThat(resultUser.getRolesList()).contains(userRole);
    }

    @Test
    @DisplayName("Debería asignar el rol de ADMIN si el email coincide")
    void findOrCreateUser_WhenUserIsAdmin_ShouldAssignAdminRole() {
        // --- 1. Arrange ---
        String adminEmail = "admin@blog.com";
        String adminUsername = "adminuser";

        when(oauthUser.getAttribute("email")).thenReturn(adminEmail);
        when(oauthUser.getAttribute("login")).thenReturn(adminUsername);

        when(userRepository.findByEmail(adminEmail)).thenReturn(Optional.empty());
        when(adminProperties.getEmail()).thenReturn(adminEmail);
        when(roleRepository.findByRole("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(UserSec.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // --- 2. Act ---
        UserSec resultUser = customOAuth2UserService.findOrCreateUser(oauthUser);

        // --- 3. Assert ---
        assertThat(resultUser).isNotNull();
        assertThat(resultUser.getRolesList()).contains(adminRole);
        assertThat(resultUser.getRolesList()).doesNotContain(userRole);
    }
}
