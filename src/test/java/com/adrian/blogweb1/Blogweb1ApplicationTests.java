package com.adrian.blogweb1;

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=password",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class Blogweb1ApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private IPermissionRepository permissionRepository;

	@Autowired
	private IRoleRepository roleRepository;

	@Autowired
	private IUserRepository userRepository;

	@Autowired
	private ObjectMapper objectMapper;

	// SOLUCIÓN DEFINITIVA: Limpieza manual antes de CADA test.
	// Esto es más rápido que @DirtiesContext y garantiza un estado 100% limpio.
	@BeforeEach
	void setUp() {
		// Limpiamos las tablas en un orden que respete las claves foráneas
		// para evitar errores de integridad referencial.
		userRepository.deleteAll();
		roleRepository.deleteAll();
		permissionRepository.deleteAll();
	}


	@Test
    @DisplayName("El contexto de la aplicación debería cargar correctamente")
	void contextLoads() {
        assertThat(mockMvc).isNotNull();
        assertThat(permissionRepository).isNotNull();
        assertThat(roleRepository).isNotNull();
        assertThat(userRepository).isNotNull();
        assertThat(objectMapper).isNotNull();
	}

	@WithMockUser(authorities = "READ")
	@Test
	@DisplayName("GET /api/permissions/{id} debería devolver 200 OK y el permiso correcto")
	void getPermissionById_IntegrationTest() throws Exception {
		// --- 1. Arrange ---
		Permission permisoGuardado = new Permission();
		permisoGuardado.setPermissionName("TEST_PERMISSION");
		Permission permisoEnDB = permissionRepository.save(permisoGuardado);
		Long idReal = permisoEnDB.getIdPermission();

		// --- 2. Act & 3. Assert ---
		mockMvc.perform(get("/api/permissions/" + idReal))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.permissionName", is("TEST_PERMISSION")));
	}

	@WithMockUser(authorities = "CREATE")
	@Test
	@DisplayName("POST /api/permissions debería crear un nuevo permiso y devolverlo")
	void createPermission_IntegrationTest() throws Exception {
		// --- 1. Arrange ---
		Permission permisoAEnviar = new Permission();
		permisoAEnviar.setPermissionName("NEW_PERMISSION");

		// --- 2. Act & 3. Assert ---
		mockMvc.perform(post("/api/permissions")
						.with(csrf()) // <-- AÑADIMOS EL TOKEN CSRF
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(permisoAEnviar)))
				.andExpect(status().isCreated()) // CORRECCIÓN: El estándar para crear es 201 Created
				.andExpect(jsonPath("$.idPermission").exists())
				.andExpect(jsonPath("$.permissionName", is("NEW_PERMISSION")));

		// --- Verificación Extra en la BD ---
		Optional<Permission> permisoEnDB = permissionRepository.findAll().stream()
				.filter(p -> p.getPermissionName().equals("NEW_PERMISSION"))
				.findFirst();

		assertThat(permisoEnDB).isPresent();
	}

	@WithMockUser(authorities = "DELETE")
	@Test
	@DisplayName("DELETE /api/permissions/{id} debería eliminar el permiso")
	void deletePermission_IntegrationTest() throws Exception {
		// --- 1. Arrange ---
		Permission permisoGuardado = new Permission();
		permisoGuardado.setPermissionName("TO_BE_DELETED");
		Permission permisoEnDB = permissionRepository.save(permisoGuardado);
		Long idReal = permisoEnDB.getIdPermission();

		// --- 2. Act ---
		mockMvc.perform(delete("/api/permissions/" + idReal)
						.with(csrf())) // <-- AÑADIMOS EL TOKEN CSRF
				.andExpect(status().isOk());

		// --- 3. Assert ---
		Optional<Permission> permisoBorrado = permissionRepository.findById(idReal);
		assertThat(permisoBorrado).isNotPresent();
	}

	@WithMockUser(authorities = "READ")
	@Test
	@DisplayName("GET /api/permissions/{id} debería devolver 404 Not Found cuando el permiso no existe")
	void getPermissionById_WhenIdDoesNotExist_ShouldReturnNotFound_IntegrationTest() throws Exception {
		long idQueNoExiste = 999L;

		mockMvc.perform(get("/api/permissions/" + idQueNoExiste))
				.andExpect(status().isNotFound());
	}

	@WithMockUser(authorities = "SOME_OTHER_PERMISSION")
	@Test
	@DisplayName("GET /api/permissions/{id} debería devolver 403 Forbidden si el usuario no tiene el permiso 'READ'")
	void getPermissionById_WithoutProperAuth_ShouldReturnForbidden_IntegrationTest() throws Exception {
		long anyId = 1L;

		mockMvc.perform(get("/api/permissions/" + anyId))
				.andExpect(status().isForbidden());
	}

	@WithMockUser(authorities = "READ") // Le damos cualquier otro permiso
	@Test
	@DisplayName("POST /api/permissions debería devolver 403 Forbidden si el usuario no tiene el permiso 'CREATE'")
	void createPermission_WithoutProperAuth_ShouldReturnForbidden_IntegrationTest() throws Exception {
		Permission permisoAEnviar = new Permission();
		permisoAEnviar.setPermissionName("SHOULD_NOT_BE_CREATED");

		mockMvc.perform(post("/api/permissions")
						.with(csrf()) // <-- AÑADIMOS EL TOKEN CSRF
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(permisoAEnviar)))
				.andExpect(status().isForbidden());
	}

	@WithMockUser(authorities = "READ") // Le damos cualquier otro permiso
	@Test
	@DisplayName("DELETE /api/permissions/{id} debería devolver 403 Forbidden si el usuario no tiene el permiso 'DELETE'")
	void deletePermission_WithoutProperAuth_ShouldReturnForbidden_IntegrationTest() throws Exception {
		long anyId = 1L;

		mockMvc.perform(delete("/api/permissions/" + anyId)
						.with(csrf())) // <-- AÑADIMOS EL TOKEN CSRF
				.andExpect(status().isForbidden());
	}

	@WithMockUser(authorities = "CREATE")
	@Test
	@DisplayName("POST /api/users debería crear un nuevo usuario y devolverlo con la contraseña encriptada")
	void createUser_IntegrationTest() throws Exception {
		// --- 1. Arrange ---
		Role rolDePrueba = new Role();
		// CORRECCIÓN: Usamos el método correcto 'setRole' en lugar de 'setName'
		rolDePrueba.setRole("ROLE_USER");
		Role rolEnDB = roleRepository.save(rolDePrueba);

		String userJson = String.format("{\"username\":\"nuevo_integracion\", \"password\":\"password_plano\", \"rolesList\":[{\"idRole\":%d}]}", rolEnDB.getIdRole());

		// --- 2. Act & 3. Assert ---
		mockMvc.perform(post("/api/users")
						.with(csrf()) // <-- AÑADIMOS EL TOKEN CSRF
						.contentType(MediaType.APPLICATION_JSON)
						.content(userJson))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.idUserSec").exists())
				.andExpect(jsonPath("$.password").doesNotExist());

		// --- Verificación Extra en la BD ---
		Optional<UserSec> usuarioEnDB = userRepository.findByUsername("nuevo_integracion");

		assertThat(usuarioEnDB).isPresent();
		assertThat(usuarioEnDB.get().getPassword()).isNotEqualTo("password_plano");
	}

	@WithMockUser(authorities = "CREATE")
	@Test
	@DisplayName("POST /api/users debería devolver 400 Bad Request si un rol no existe")
	void createUser_WhenRoleDoesNotExist_ShouldReturnBadRequest() throws Exception {
		// --- 1. Arrange ---
		String userJson = "{\"username\":\"test_user\", \"password\":\"password123\", \"rolesList\":[{\"idRole\":999}]}";

		// --- 2. Act & 3. Assert ---
		mockMvc.perform(post("/api/users")
						.with(csrf()) // <-- AÑADIMOS EL TOKEN CSRF
						.contentType(MediaType.APPLICATION_JSON)
						.content(userJson))
				.andExpect(status().isBadRequest());
	}

}
