package com.adrian.blogweb1.controllerTest;

import com.adrian.blogweb1.controller.PermissionController;
import com.adrian.blogweb1.exception.GlobalExceptionHandler;
import com.adrian.blogweb1.exception.ResourceNotFoundException;
import com.adrian.blogweb1.model.Permission;
import com.adrian.blogweb1.service.IPermissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {PermissionController.class, GlobalExceptionHandler.class})
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private IPermissionService permissionService;

    @Test
    @DisplayName("GET /api/permissions/{id} - Debería devolver 200 OK y un permiso cuando el ID existe")
    @WithMockUser(authorities = "READ")
    void getPermissionById_WhenIdExists_ShouldReturnOkAndPermission() throws Exception {
        // --- 1. Arrange ---
        long permissionId = 1L;
        Permission permisoDePrueba = new Permission();
        permisoDePrueba.setIdPermission(permissionId);
        permisoDePrueba.setPermissionName("READ");

        when(permissionService.findById(permissionId)).thenReturn(Optional.of(permisoDePrueba));

        // --- 2. Act & 3. Assert ---
        mockMvc.perform(get("/api/permissions/{id}", permissionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPermission", is(1)))
                .andExpect(jsonPath("$.permissionName", is("READ")));
    }

    @Test
    @DisplayName("GET /api/permissions/{id} - Debería devolver 404 Not Found cuando el ID no existe")
    @WithMockUser(authorities = "READ")
    void getPermissionById_WhenIdDoesNotExist_ShouldReturnNotFound() throws Exception {
        // --- 1. Arrange ---
        long permissionIdQueNoExiste = 99L;
        when(permissionService.findById(permissionIdQueNoExiste)).thenReturn(Optional.empty());

        // --- 2. Act & 3. Assert ---
        mockMvc.perform(get("/api/permissions/{id}", permissionIdQueNoExiste))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/permissions - Debería devolver 201 Created y el permiso creado")
    @WithMockUser(authorities = "CREATE")
    void createPermission_ShouldReturnCreatedAndNewPermission() throws Exception {
        // --- 1. Arrange ---
        Permission permisoAEnviar = new Permission();
        permisoAEnviar.setPermissionName("WRITE");

        Permission permisoGuardado = new Permission();
        permisoGuardado.setIdPermission(1L);
        permisoGuardado.setPermissionName("WRITE");

        when(permissionService.save(any(Permission.class))).thenReturn(permisoGuardado);

        // --- 2. Act & 3. Assert ---
        mockMvc.perform(post("/api/permissions")
                        .with(csrf()) // <-- SOLUCIÓN: Añadimos el token CSRF
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(permisoAEnviar)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idPermission", is(1)))
                .andExpect(jsonPath("$.permissionName", is("WRITE")));
    }

    @Test
    @DisplayName("DELETE /api/permissions/{id} - Debería devolver 200 OK cuando se elimina un permiso existente")
    @WithMockUser(authorities = "DELETE")
    void deletePermission_WhenIdExists_ShouldReturnOk() throws Exception {
        // --- 1. Arrange ---
        long permissionId = 1L;
        doNothing().when(permissionService).deletePermission(permissionId);

        // --- 2. Act & 3. Assert ---
        mockMvc.perform(delete("/api/permissions/{id}", permissionId)
                        .with(csrf())) // <-- SOLUCIÓN: Añadimos el token CSRF
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Permiso eliminado correctamente")));

        verify(permissionService, times(1)).deletePermission(permissionId);
    }

    @Test
    @DisplayName("DELETE /api/permissions/{id} - Debería devolver 404 Not Found si el permiso no existe")
    @WithMockUser(authorities = "DELETE")
    void deletePermission_WhenIdDoesNotExist_ShouldReturnNotFound() throws Exception {
        // --- 1. Arrange ---
        long permissionIdQueNoExiste = 99L;
        doThrow(new ResourceNotFoundException("Permiso no encontrado")).when(permissionService).deletePermission(permissionIdQueNoExiste);

        // --- 2. Act & 3. Assert ---
        mockMvc.perform(delete("/api/permissions/{id}", permissionIdQueNoExiste)
                        .with(csrf())) // <-- SOLUCIÓN: Añadimos el token CSRF
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Permiso no encontrado")));
    }

    @Test
    @DisplayName("PUT /api/permissions/{id} - Debería devolver 200 OK y el permiso actualizado")
    @WithMockUser(authorities = "UPDATE")
    void updatePermission_WhenIdExists_ShouldReturnOkAndUpdatedPermission() throws Exception {
        // --- 1. Arrange ---
        long permissionId = 1L;
        Permission detallesNuevos = new Permission();
        detallesNuevos.setPermissionName("SUPER_ADMIN_PERMISSION");

        Permission permisoActualizado = new Permission();
        permisoActualizado.setIdPermission(permissionId);
        permisoActualizado.setPermissionName("SUPER_ADMIN_PERMISSION");

        when(permissionService.updatePermission(eq(permissionId), any(Permission.class))).thenReturn(permisoActualizado);

        // --- 2. Act & 3. Assert ---
        mockMvc.perform(put("/api/permissions/{id}", permissionId)
                        .with(csrf()) // <-- SOLUCIÓN: Añadimos el token CSRF
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(detallesNuevos)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idPermission", is(1)))
                .andExpect(jsonPath("$.permissionName", is("SUPER_ADMIN_PERMISSION")));

        verify(permissionService, times(1)).updatePermission(eq(permissionId), any(Permission.class));
    }

    @Test
    @DisplayName("PUT /api/permissions/{id} - Debería devolver 404 Not Found si el permiso a actualizar no existe")
    @WithMockUser(authorities = "UPDATE")
    void updatePermission_WhenIdDoesNotExist_ShouldReturnNotFound() throws Exception {
        // --- 1. Arrange ---
        long permissionIdQueNoExiste = 99L;
        Permission detallesNuevos = new Permission();
        detallesNuevos.setPermissionName("UN_NOMBRE_CUALQUIERA");

        when(permissionService.updatePermission(eq(permissionIdQueNoExiste), any(Permission.class)))
                .thenThrow(new ResourceNotFoundException("Permiso no encontrado para actualizar"));

        // --- 2. Act & 3. Assert ---
        mockMvc.perform(put("/api/permissions/{id}", permissionIdQueNoExiste)
                        .with(csrf()) // <-- SOLUCIÓN: Añadimos el token CSRF
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(detallesNuevos)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", is("Permiso no encontrado para actualizar")));
    }
}