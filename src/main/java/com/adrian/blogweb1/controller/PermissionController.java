package com.adrian.blogweb1.controller;


import com.adrian.blogweb1.exception.ResourceNotFoundException;
import com.adrian.blogweb1.model.Permission;
import com.adrian.blogweb1.service.IPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping
    @PreAuthorize("hasRole('PROFESOR') or hasRole('ESTUDIANTE') and hasAuthority('SELECT')")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        List<Permission> permissions = permissionService.findAll(); // Especifica el tipo <Permission>
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/{idPermission}") // Usa el mismo nombre que el parámetro del método
    @PreAuthorize("hasRole('PROFESOR') or hasRole('ESTUDIANTE') and hasAuthority('SELECT')")
    public ResponseEntity<Permission> getPermissionById(@PathVariable Long idPermission) {
        Optional<Permission> permission = permissionService.findById(idPermission);
        return permission.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Permission> createPermission( @RequestBody Permission permission) {
        Permission newPermission = permissionService.save(permission);
        return ResponseEntity.ok(newPermission);
    }

    @PutMapping("/{id}")
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

    // @UpdatePermission lo tengo junto con rol  updateRolePermission

    @DeleteMapping("/{id}")
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


