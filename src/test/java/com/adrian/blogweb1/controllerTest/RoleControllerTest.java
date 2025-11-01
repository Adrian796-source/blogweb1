package com.adrian.blogweb1.controllerTest;

import com.adrian.blogweb1.controller.RoleController;
import com.adrian.blogweb1.exception.GlobalExceptionHandler;
import com.adrian.blogweb1.exception.ResourceNotFoundException;
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

import java.util.HashSet;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
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
}