package com.adrian.blogweb1.controller;


import com.adrian.blogweb1.exception.ResourceNotFoundException;
import com.adrian.blogweb1.model.Permission;
import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.service.IPermissionService;
import com.adrian.blogweb1.service.IRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {


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
        Set<Permission> permissionList = new HashSet<>();
        Permission readPermission;

        // Recuperar la Permission/s por su ID
        for (Permission per : role.getPermissionsList()) {
            readPermission = permissionService.findById(per.getIdPermission()).orElse(null);
            if (readPermission != null) {
                //si encuentro, guardo en la lista
                permissionList.add(readPermission);
            }
        }

        role.setPermissionsList(permissionList);
        Role newRole = roleService.save(role);
        return ResponseEntity.ok(newRole);
    }

    /**
     * Actualiza los permisos de un rol existente.
     * Requiere el permiso 'UPDATE'. Solo accesible por ADMIN.
     */
    @PutMapping("/{idRole}/permissions")
    @PreAuthorize("hasAuthority('UPDATE')")
    @Transactional
    public ResponseEntity<Role> updateRolePermissions(
            @PathVariable Long idRole,
            @RequestBody Set<Permission> permissions) {
        try {
            Role updatedRole = roleService.updateRolePermissions(idRole, permissions);
            return ResponseEntity.ok(updatedRole);
        } catch (RuntimeException e) {
            System.out.println("Error: " + e.getMessage()); // Log para depuración
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Elimina un rol por su ID.
     * Requiere el permiso 'DELETE'. Solo accesible por ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE')")
    public ResponseEntity<?> deleteRole(@PathVariable Long id) {
        try {
            roleService.deleteRole(id);
            return ResponseEntity.ok().body("Rol eliminado correctamente");
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body("Error al eliminar el rol: " + ex.getMessage());
        }
    }

}


