package com.adrian.blogweb1.controller;


import com.adrian.blogweb1.exception.ResourceNotFoundException;
import com.adrian.blogweb1.model.Permission;
import com.adrian.blogweb1.service.IPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
public class PermissionController {


    private final IPermissionService permissionService;


    /**
     * Obtiene todos los permisos.
     * Requiere el permiso 'READ'. Accesible por ADMIN y USER.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        List<Permission> permissions = permissionService.findAll(); // Especifica el tipo <Permission>
        return ResponseEntity.ok(permissions);
    }

    /**
     * Obtiene un permiso por su ID.
     * Requiere el permiso 'READ'. Accesible por ADMIN y USER.
     */
    @GetMapping("/{idPermission}") // Usa el mismo nombre que el parámetro del método
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<Permission> getPermissionById(@PathVariable Long idPermission) {
        Optional<Permission> permission = permissionService.findById(idPermission);
        return permission.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Crea un nuevo permiso.
     * Requiere el permiso 'CREATE'. Solo accesible por ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CREATE')")
    @Transactional
    public ResponseEntity<Permission> createPermission( @RequestBody Permission permission) {
        Permission newPermission = permissionService.save(permission);
        return ResponseEntity.ok(newPermission);
    }

    /**
     * Actualiza un permiso existente.
     * Requiere el permiso 'UPDATE'. Solo accesible por ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('UPDATE')")
    public ResponseEntity<?> updatePermission(
            @PathVariable Long id,
            @RequestBody Permission permissionDetails) {
        try {
            Permission updatedPermission = permissionService.updatePermission(id, permissionDetails);
            return ResponseEntity.ok(updatedPermission);
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + ex.getMessage());
        }
    }

    /**
     * Elimina un permiso por su ID.
     * Requiere el permiso 'DELETE'. Solo accesible por ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE')")
    public ResponseEntity<?> deletePermission(@PathVariable Long id) {
        try {
            permissionService.deletePermission(id);
            return ResponseEntity.ok()
                    .body(Map.of(
                            "status", "success",
                            "message", "Permiso eliminado correctamente"
                    ));
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "status", "error",
                            "message", ex.getMessage()
                    ));
        } catch (Exception ex) {
            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "status", "error",
                            "message", "Error al eliminar el permiso: " + ex.getMessage()
                    ));
        }
    }

}


