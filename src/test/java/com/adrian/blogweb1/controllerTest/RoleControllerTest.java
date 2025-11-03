package com.adrian.blogweb1.controllerTest;

import com.adrian.blogweb1.controller.RoleController;
import com.adrian.blogweb1.exception.GlobalExceptionHandler;
import com.adrian.blogweb1.exception.ResourceNotFoundException;
import com.adrian.blogweb1.model.Permission;
import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.service.IPermissionService;
import com.adrian.blogweb1.service.IRoleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {RoleController.class, GlobalExceptionHandler.class})
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IRoleService roleService;

    @MockBean
    private IPermissionService permissionService;

    // --- INICIO DE LA SOLUCIÓN: NUEVOS TESTS PARA COBERTURA ---

    @Test
    @DisplayName("GET /api/roles - Debería devolver 200 OK y una lista de todos los roles")
    @WithMockUser(authorities = "READ")
    void getAllRoles_ShouldReturnListOfRoles() throws Exception {
        // --- 1. Arrange ---
        Role role1 = new Role();
        role1.setRole("ROLE_USER");
        Role role2 = new Role();
        role2.setRole("ROLE_ADMIN");

        when(roleService.findAll()).thenReturn(List.of(role1, role2));

        // --- 2. Act & 3. Assert ---
        mockMvc.perform(get("/api/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].role", is("ROLE_USER")));
    }

    @Test
    @DisplayName("GET /api/roles/{id} - Debería devolver 200 OK y un rol cuando el ID existe")
    @WithMockUser(authorities = "READ")
    void getRoleById_WhenIdExists_ShouldReturnOkAndRole() throws Exception {
        // --- 1. Arrange ---
        long roleId = 1L;
        Role rolDePrueba = new Role();
        rolDePrueba.setIdRole(roleId);
        rolDePrueba.setRole("ROLE_USER");

        when(roleService.findById(roleId)).thenReturn(Optional.of(rolDePrueba));

        // --- 2. Act & 3. Assert ---
        mockMvc.perform(get("/api/roles/{idRole}", roleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("ROLE_USER")));
    }

    @Test
    @DisplayName("GET /api/roles/{id} - Debería devolver 404 Not Found cuando el ID del rol no existe")
    @WithMockUser(authorities = "READ")
    void getRoleById_WhenIdDoesNotExist_ShouldReturnNotFound() throws Exception {
        // --- 1. Arrange ---
        long roleIdQueNoExiste = 99L;
        when(roleService.findById(roleIdQueNoExiste)).thenReturn(Optional.empty());

        // --- 2. Act & 3. Assert ---
        mockMvc.perform(get("/api/roles/{idRole}", roleIdQueNoExiste))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/roles - Debería devolver 201 Created y el rol creado al guardar uno nuevo")
    @WithMockUser(authorities = "CREATE")
    void createRole_ShouldReturnCreatedAndNewRole() throws Exception {
        // --- 1. Arrange ---
        Role rolAEnviar = new Role();
        rolAEnviar.setRole("ROLE_GUEST");
        rolAEnviar.setPermissionsList(new HashSet<>());

        Role rolGuardado = new Role();
        rolGuardado.setIdRole(3L);
        rolGuardado.setRole("ROLE_GUEST");

        when(roleService.save(any(Role.class))).thenReturn(rolGuardado);

        // --- 2. Act & 3. Assert ---
        mockMvc.perform(post("/api/roles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rolAEnviar)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idRole", is(3)))
                .andExpect(jsonPath("$.role", is("ROLE_GUEST")));
    }

    @Test
    @DisplayName("POST /api/roles - Debería crear un rol con permisos y devolver 201 Created")
    @WithMockUser(authorities = "CREATE")
    void createRole_WithPermissions_ShouldReturnCreated() throws Exception {
        // --- 1. Arrange ---
        Permission readPermission = new Permission();
        readPermission.setIdPermission(1L);
        readPermission.setPermissionName("READ");

        Role rolAEnviar = new Role();
        rolAEnviar.setRole("ROLE_MODERATOR");
        rolAEnviar.setPermissionsList(Collections.singleton(readPermission));

        Role rolGuardado = new Role();
        rolGuardado.setIdRole(4L);
        rolGuardado.setRole("ROLE_MODERATOR");
        rolGuardado.setPermissionsList(Collections.singleton(readPermission));

        when(permissionService.findById(1L)).thenReturn(Optional.of(readPermission));
        when(roleService.save(any(Role.class))).thenReturn(rolGuardado);

        // --- 2. Act & 3. Assert ---
        mockMvc.perform(post("/api/roles")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rolAEnviar)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("ROLE_MODERATOR")))
                .andExpect(jsonPath("$.permissionsList[0].permissionName", is("READ")));
    }

    @Test
    @DisplayName("DELETE /api/roles/{id} - Debería devolver 200 OK cuando se elimina un rol existente")
    @WithMockUser(authorities = "DELETE")
    void deleteRole_WhenIdExists_ShouldReturnOk() throws Exception {
        // --- 1. Arrange ---
        long roleId = 1L;
        doNothing().when(roleService).deleteRole(roleId);

        // --- 2. Act & 3. Assert ---
        mockMvc.perform(delete("/api/roles/{id}", roleId)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Rol eliminado correctamente")));

        verify(roleService, times(1)).deleteRole(roleId);
    }

    @Test
    @DisplayName("DELETE /api/roles/{id} - Debería devolver 404 Not Found cuando se intenta eliminar un rol que no existe")
    @WithMockUser(authorities = "DELETE")
    void deleteRole_WhenIdDoesNotExist_ShouldReturnNotFound() throws Exception {
        // --- 1. Arrange ---
        long roleIdQueNoExiste = 99L;
        doThrow(new ResourceNotFoundException("Rol no encontrado")).when(roleService).deleteRole(roleIdQueNoExiste);

        // --- 2. Act & 3. Assert ---
        mockMvc.perform(delete("/api/roles/{id}", roleIdQueNoExiste)
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Rol no encontrado")));
    }

    @Test
    @DisplayName("PUT /api/roles/{id}/permissions - Debería devolver 404 si el rol no existe")
    @WithMockUser(authorities = "UPDATE")
    void updateRolePermissions_whenRoleDoesNotExist_shouldReturnNotFound() throws Exception {
        // --- 1. Arrange ---
        long nonExistentRoleId = 99L;

        // Simulamos que el servicio lanza la excepción
        when(roleService.updateRolePermissions(eq(nonExistentRoleId), any()))
                .thenThrow(new ResourceNotFoundException("Rol no encontrado con ID: " + nonExistentRoleId));

        // --- 2. Act & 3. Assert ---
        mockMvc.perform(put("/api/roles/{idRole}/permissions", nonExistentRoleId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[]")) // Enviamos una lista vacía de permisos
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Rol no encontrado con ID: 99")));
    }

    @Test
    @DisplayName("DELETE /api/roles/{id} - Debería devolver 500 si ocurre un error inesperado")
    @WithMockUser(authorities = "DELETE")
    void deleteRole_whenUnexpectedError_shouldReturnInternalServerError() throws Exception {
        // --- 1. Arrange ---
        long roleId = 1L;

        // Simulamos un error genérico (diferente de ResourceNotFoundException)
        doThrow(new RuntimeException("Error de base de datos simulado")).when(roleService).deleteRole(roleId);

        // --- 2. Act & 3. Assert ---
        mockMvc.perform(delete("/api/roles/{id}", roleId)
                        .with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", containsString("Error al eliminar el rol")));
    }
}