package com.adrian.blogweb1.controller;


import com.adrian.blogweb1.exception.ResourceNotFoundException;
import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.model.UserSec;
import com.adrian.blogweb1.service.IRoleService;
import com.adrian.blogweb1.service.IUserService;
import jakarta.validation.Valid;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {


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
    public ResponseEntity<?> createUser(@RequestBody UserSec userSec) {
        try {
            // Verifica si el campo password está presente y no está vacío
            if (userSec.getPassword() == null || userSec.getPassword().isEmpty()) {
                return ResponseEntity.badRequest().body("El campo 'password' no puede estar vacío");
            }
            // Verifica si el campo rolesList está presente y no está vacío
            if (userSec.getRolesList() == null || userSec.getRolesList().isEmpty()) {
                return ResponseEntity.badRequest().body("El campo 'rolesList' no puede estar vacío");
            }

            Set<Role> roleList = new HashSet<>();
            Role readRole;

            // Encriptamos contraseña
            userSec.setPassword(userService.encryptPassword(userSec.getPassword()));

            // Recuperar los roles por su ID
            for (Role role : userSec.getRolesList()) {
                readRole = roleService.findById(role.getIdRole())
                        .orElseThrow(() -> new RuntimeException("Rol no encontrado con ID: " + role.getIdRole()));
                roleList.add(readRole);
            }

            // Asigna los roles al usuario y lo guarda en la base de datos
            userSec.setRolesList(roleList);
            UserSec newUser = userService.save(userSec);

            // Mensaje en consola: "Usuario creado"
            System.out.println("Usuario creado");

            // Devuelve una respuesta 201 Created con el nuevo usuario
            return ResponseEntity.status(HttpStatus.CREATED).body(newUser);
        } catch (Exception e) {
            // Logear la excepción
            System.err.println("Error al crear el usuario: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al crear el usuario");
        }
    }

    /**
     * Actualiza un usuario existente.
     * Requiere el permiso 'UPDATE'. Solo accesible por ADMIN.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('UPDATE')")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserSec userDetails) {

        try {
            // Llamar al servicio para actualizar el usuario
            UserSec updatedUser = userService.updateUser(id, userDetails);

            // Devolver respuesta exitosa con el usuario actualizado
            return ResponseEntity.ok(updatedUser);

        } catch (RuntimeException ex) {
            // Manejar errores específicos
            if (ex.getMessage().contains("Usuario no encontrado")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Error: " + ex.getMessage());
            } else if (ex.getMessage().contains("Rol no encontrado")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Error: " + ex.getMessage());
            }

            // Manejar otros errores genéricos
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error inesperado: " + ex.getMessage());
        }
    }

    /**
     * Elimina un usuario por su ID.
     * Requiere el permiso 'DELETE'. Solo accesible por ADMIN.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DELETE')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userService.deleteUser(id);
            return ResponseEntity.ok().build();
        } catch (ResourceNotFoundException ex) {
            return ResponseEntity.notFound().build();
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Error al eliminar el usuario");
        }
    }

}


