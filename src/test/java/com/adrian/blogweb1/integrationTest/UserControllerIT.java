package com.adrian.blogweb1.integrationTest;

import com.adrian.blogweb1.model.Permission;
import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.model.UserSec;
import com.adrian.blogweb1.repository.IPermissionRepository;
import com.adrian.blogweb1.repository.IRoleRepository;
import com.adrian.blogweb1.repository.IUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
public class UserControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private IRoleRepository roleRepository;

    @Autowired
    private IPermissionRepository permissionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/users debería devolver 403 Forbidden para un usuario normal")
    void getUsers_WhenUserIsNormalUser_ShouldReturnForbidden() throws Exception {
        // Arrange: Crear un usuario con rol USER
        Role userRole = new Role();
        userRole.setRole("USER");
        Role savedUserRole = roleRepository.save(userRole);

        UserSec normalUser = new UserSec();
        normalUser.setUsername("normaluser");
        normalUser.setPassword(passwordEncoder.encode("password"));
        normalUser.setEnabled(true);
        normalUser.setRolesList(Set.of(savedUserRole));
        userRepository.save(normalUser);

        // Act 1: Obtener el token del usuario normal
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "normaluser", "password", "password"))))
                .andExpect(status().isOk())
                .andReturn();

        String jwtToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("jwt").asText();

        // Act 2 & Assert: Intentar acceder a la lista de usuarios con el token del usuario normal
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/users debería devolver 200 OK para un usuario ADMIN")
    void getUsers_WhenUserIsAdmin_ShouldReturnOk() throws Exception {
        // Arrange: Crear un usuario con rol ADMIN
        // Asumimos que el rol ADMIN tiene los permisos necesarios para leer usuarios.
        Permission readAllUsersPermission = new Permission();
        readAllUsersPermission.setPermissionName("UPDATE"); // Usamos el permiso específico para esta acción
        // Guardamos y obtenemos la instancia gestionada por la base de datos
        Permission savedReadAllUsersPermission = permissionRepository.save(readAllUsersPermission);

        Role adminRole = new Role();
        adminRole.setRole("ADMIN");
        adminRole.setPermissionsList(Set.of(savedReadAllUsersPermission)); // Usamos la instancia gestionada del permiso
        Role savedAdminRole = roleRepository.save(adminRole);

        UserSec adminUser = new UserSec();
        adminUser.setUsername("adminuser");
        adminUser.setPassword(passwordEncoder.encode("password"));
        adminUser.setEnabled(true);
        adminUser.setRolesList(Set.of(savedAdminRole));
        userRepository.save(adminUser);

        // Act 1: Obtener el token del usuario admin
        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", "adminuser", "password", "password"))))
                .andExpect(status().isOk())
                .andReturn();

        String jwtToken = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("jwt").asText();

        // Act 2 & Assert: Acceder a la lista de usuarios con el token de admin
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + jwtToken))
                .andExpect(status().isOk());
    }
}