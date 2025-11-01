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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
public class AuthenticationControllerIT {

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
    @DisplayName("POST /auth/login debería autenticar al usuario y devolver un token JWT")
    void login_WithValidCredentials_ShouldReturnJwt() throws Exception {
        // Arrange: Creamos los permisos y roles necesarios para este test específico.
        // 1. Crear el permiso 'READ'
        Permission readPermission = new Permission();
        readPermission.setPermissionName("READ");
        Permission savedReadPermission = permissionRepository.save(readPermission);

        // 2. Crear el rol 'USER' y asignarle el permiso 'READ'
        Role userRole = new Role();
        userRole.setRole("USER");
        userRole.setPermissionsList(Set.of(savedReadPermission));
        Role savedUserRole = roleRepository.save(userRole);

        // Crear un usuario de prueba y ASIGNARLE el rol existente
        UserSec testUser = new UserSec();
        testUser.setUsername("testuser");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setEnabled(true);
        testUser.setRolesList(Set.of(savedUserRole));
        userRepository.save(testUser);

        // Crear el cuerpo de la petición de login
        Map<String, String> loginRequest = Map.of(
                "username", "testuser",
                "password", "password123"
        );

        // Act & Assert: Realizar la petición POST a /auth/login
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jwt").exists()) // Verificar que la respuesta contiene un campo 'jwt'
                .andReturn();

        // Extraer el token de la respuesta
        String responseBody = result.getResponse().getContentAsString();
        String jwtToken = objectMapper.readTree(responseBody).get("jwt").asText();

        // Verificar que el token no está vacío
        assertThat(jwtToken).isNotBlank();

        // (Paso extra) Usar el token para acceder a un recurso protegido
        mockMvc.perform(get("/api/authors")
                        .header("Authorization", "Bearer " + jwtToken)) // Añadir el token al header
                .andExpect(status().isOk()); // Esperar una respuesta exitosa

    }

    @Test
    @DisplayName("POST /auth/login con credenciales inválidas debería devolver 401 Unauthorized")
    void login_WithInvalidCredentials_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        Map<String, String> loginRequest = Map.of(
                "username", "nouser",
                "password", "wrongpassword"
        );

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }
}
