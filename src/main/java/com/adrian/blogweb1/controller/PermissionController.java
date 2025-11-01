package com.adrian.blogweb1.controller;


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

    // --- CORRECCIÓN: Constantes para claves y valores de respuesta JSON ---
    private static final String STATUS_KEY = "status";
    private static final String MESSAGE_KEY = "message";
    private static final String STATUS_ERROR = "error";
    private static final String STATUS_SUCCESS = "success";

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
        return new ResponseEntity<>(newPermission, HttpStatus.CREATED);
    }

    /**
     * Actualiza un permiso existente.
     * Requiere el permiso 'UPDATE'. Solo accesible por ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('UPDATE')")
    public ResponseEntity<Object> updatePermission(
            @PathVariable Long id,
            @RequestBody Permission permissionDetails) {
        Permission updatedPermission = permissionService.updatePermission(id, permissionDetails);
        return ResponseEntity.ok(updatedPermission);
    }

    /**
     * Elimina un permiso por su ID.
     * Requiere el permiso 'DELETE'. Solo accesible por ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE')")
    public ResponseEntity<Map<String, String>> deletePermission(@PathVariable Long id) {
        permissionService.deletePermission(id);
        return ResponseEntity.ok(Map.of(
                STATUS_KEY, STATUS_SUCCESS,
                MESSAGE_KEY, "Permiso eliminado correctamente"
        ));
    }

}
