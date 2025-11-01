package com.adrian.blogweb1.controller;


import com.adrian.blogweb1.exception.ResourceNotFoundException;
import com.adrian.blogweb1.model.Permission;
import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.service.IPermissionService;
import com.adrian.blogweb1.service.IRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private static final String STATUS_KEY = "status";
    private static final String MESSAGE_KEY = "message";
    private static final String STATUS_ERROR = "error";
    private static final String STATUS_SUCCESS = "success";

    private final IRoleService roleService;
    private final IPermissionService permissionService;

    /**
     * Obtiene todos los roles.
     * Requiere el permiso 'READ'. Accesible por ADMIN y USER.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<List<Role>> getAllRoles() {
        List<Role> roles = roleService.findAll();
        return ResponseEntity.ok(roles);
    }

    /**
     * Obtiene un rol por su ID.
     * Requiere el permiso 'READ'. Accesible por ADMIN y USER.
     */
    @GetMapping("/{idRole}")
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<Role> getRoleById(@PathVariable Long idRole) {
        Optional<Role> role = roleService.findById(idRole);
        return role.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Crea un nuevo rol.
     * Requiere el permiso 'CREATE'. Solo accesible por ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CREATE')")
    @Transactional
    public ResponseEntity<Role> createRole(@RequestBody Role role) {
        // Lógica mejorada para recuperar los permisos y evitar detached entities
        if (role.getPermissionsList() != null && !role.getPermissionsList().isEmpty()) {
            Set<Permission> managedPermissions = role.getPermissionsList().stream()
                    .map(Permission::getIdPermission) // Extraemos los IDs de los permisos de la petición
                    .map(permissionService::findById) // Buscamos cada permiso en la BD
                    .filter(Optional::isPresent)      // Filtramos los que no se encontraron
                    .map(Optional::get)               // Obtenemos el objeto Permission del Optional
                    .collect(Collectors.toSet());     // Los recolectamos en un nuevo Set
            role.setPermissionsList(managedPermissions);
        }

        Role newRole = roleService.save(role);

        // Devolvemos 201 Created, que es el estándar para la creación de recursos.
        return new ResponseEntity<>(newRole, HttpStatus.CREATED);
    }

    /**
     * Actualiza los permisos de un rol existente.
     * Requiere el permiso 'UPDATE'. Solo accesible por ADMIN.
     */
    @PutMapping("/{idRole}/permissions")
    @PreAuthorize("hasAuthority('UPDATE')")
    @Transactional
    public ResponseEntity<Object> updateRolePermissions(
            @PathVariable Long idRole,
            @RequestBody Set<Permission> permissions) {
        try {
            Role updatedRole = roleService.updateRolePermissions(idRole, permissions);
            return ResponseEntity.ok(updatedRole);
        } catch (ResourceNotFoundException e) {
            log.warn("Intento de actualizar un rol no encontrado. ID: {}", idRole, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    STATUS_KEY, STATUS_ERROR,
                    MESSAGE_KEY, e.getMessage()
            ));
        }
    }

    /**
     * Elimina un rol por su ID.
     * Requiere el permiso 'DELETE'. Solo accesible por ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE')")
    public ResponseEntity<Map<String, String>> deleteRole(@PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            return ResponseEntity.ok(Map.of(
                    STATUS_KEY, STATUS_SUCCESS,
                    MESSAGE_KEY, "Rol eliminado correctamente"
            ));
        } catch (ResourceNotFoundException ex) {
            log.warn("Intento de eliminar un rol no encontrado. ID: {}", id, ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            STATUS_KEY, STATUS_ERROR,
                            MESSAGE_KEY, ex.getMessage()
                    ));
        } catch (Exception ex) {
            log.error("Error inesperado al eliminar el rol ID: {}", id, ex);
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            STATUS_KEY, STATUS_ERROR,
                            MESSAGE_KEY, "Error al eliminar el rol: " + ex.getMessage()
                    ));
        }
    }

}
