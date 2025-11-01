package com.adrian.blogweb1.controller;


import com.adrian.blogweb1.exception.ResourceNotFoundException;
import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.model.UserSec;
import com.adrian.blogweb1.service.IRoleService;
import com.adrian.blogweb1.service.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private static final String STATUS_KEY = "status";
    private static final String MESSAGE_KEY = "message";
    private static final String STATUS_ERROR = "error";
    private static final String STATUS_SUCCESS = "success";

    private final IUserService userService;
    private final IRoleService roleService;

    /**
     * Obtiene todos los usuarios. Operación sensible, solo para administradores.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('UPDATE')")
    public ResponseEntity<List<UserSec>> getAllUsers() {
        List<UserSec> users = userService.findAll();
        return ResponseEntity.ok(users);
    }

    /**
     * Obtiene un usuario por su ID. Operación sensible, solo para administradores.
     */
    @GetMapping("/{idUserSec}")
    @PreAuthorize("hasAuthority('UPDATE')")
    public ResponseEntity<UserSec> getUserById(@PathVariable Long idUserSec) {
        Optional<UserSec> user = userService.findById(idUserSec);
        return user.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }


    /**
     * Crea un nuevo usuario.
     * Requiere el permiso 'CREATE'. Solo accesible por ADMIN.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('CREATE')")
    @Transactional
    public ResponseEntity<Object> createUser(@RequestBody UserSec userSec) {
        try {
            if (userSec.getPassword() == null || userSec.getPassword().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(STATUS_KEY, STATUS_ERROR, MESSAGE_KEY, "El campo 'password' no puede estar vacío"));
            }
            if (userSec.getRolesList() == null || userSec.getRolesList().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(STATUS_KEY, STATUS_ERROR, MESSAGE_KEY, "El campo 'rolesList' no puede estar vacío"));
            }

            Set<Role> roleList = new HashSet<>();
            userSec.setPassword(userService.encryptPassword(userSec.getPassword()));

            for (Role role : userSec.getRolesList()) {
                Role readRole = roleService.findById(role.getIdRole())
                        .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + role.getIdRole()));
                roleList.add(readRole);
            }

            userSec.setRolesList(roleList);
            UserSec newUser = userService.save(userSec);

            log.info("Usuario creado con ID: {}", newUser.getIdUserSec());

            return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
        } catch (ResourceNotFoundException e) {
            log.warn("Error de cliente al crear usuario: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(STATUS_KEY, STATUS_ERROR, MESSAGE_KEY, e.getMessage()));
        } catch (Exception e) {
            log.error("Error interno al crear el usuario", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(STATUS_KEY, STATUS_ERROR, MESSAGE_KEY, "Error interno al crear el usuario"));
        }
    }

    /**
     * Actualiza un usuario existente.
     * Requiere el permiso 'UPDATE'. Solo accesible por ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('UPDATE')")
    public ResponseEntity<Object> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserSec userDetails) {

        try {
            UserSec updatedUser = userService.updateUser(id, userDetails);
            return ResponseEntity.ok(updatedUser);
        } catch (ResourceNotFoundException ex) {
            log.warn("Intento de actualizar un usuario no encontrado. ID: {}", id, ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(STATUS_KEY, STATUS_ERROR, MESSAGE_KEY, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Error inesperado al actualizar el usuario ID: {}", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(STATUS_KEY, STATUS_ERROR, MESSAGE_KEY, "Error inesperado al actualizar el usuario"));
        }
    }

    /**
     * Elimina un usuario por su ID.
     * Requiere el permiso 'DELETE'. Solo accesible por ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE')")
    public ResponseEntity<Map<String, String>> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            log.info("Usuario con ID: {} eliminado correctamente", id);
            return ResponseEntity.ok(Map.of(STATUS_KEY, STATUS_SUCCESS, MESSAGE_KEY, "Usuario eliminado correctamente"));
        } catch (ResourceNotFoundException ex) {
            log.warn("Intento de eliminar un usuario no encontrado. ID: {}", id, ex);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(STATUS_KEY, STATUS_ERROR, MESSAGE_KEY, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Error inesperado al eliminar el usuario ID: {}", id, ex);
            return ResponseEntity.internalServerError().body(Map.of(STATUS_KEY, STATUS_ERROR, MESSAGE_KEY, "Error al eliminar el usuario"));
        }
    }

}
