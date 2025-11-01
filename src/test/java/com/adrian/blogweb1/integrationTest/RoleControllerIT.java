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
public class RoleControllerIT {

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
    @DisplayName("POST /api/roles/create debería crear un nuevo rol para un usuario ADMIN")
    void createRole_WhenUserIsAdmin_ShouldCreateRole() throws Exception {
        // Arrange: Crear un usuario ADMIN con el permiso para crear roles
        // Asumimos que el permiso se llama 'CREATE' o 'CREATE_ROLE'
        Permission createPermission = new Permission();
        createPermission.setPermissionName("CREATE");
        Permission savedCreatePermission = permissionRepository.save(createPermission);

        Role adminRole = new Role();
        adminRole.setRole("ADMIN");
        adminRole.setPermissionsList(Set.of(savedCreatePermission));
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

        // Act 2 & Assert: Intentar crear un nuevo rol con el token de admin
        Role newRole = new Role();
        newRole.setRole("SUPERVISOR");

        mockMvc.perform(post("/api/roles/create")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRole)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /api/roles/create debería devolver 403 Forbidden para un usuario normal")
    void createRole_WhenUserIsNormalUser_ShouldReturnForbidden() throws Exception {
        // Arrange: Crear un usuario normal y obtener su token
        // (Este usuario no tiene el permiso 'CREATE')
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

        // Act & Assert: Intentar crear un rol con el token del usuario normal

        Role newRole = new Role();
        newRole.setRole("ROL_PROHIBIDO");

        mockMvc.perform(post("/api/roles")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRole)))
                .andExpect(status().isForbidden());
    }

}

