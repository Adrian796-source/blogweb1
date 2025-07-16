package com.adrian.blogweb1.service;

import com.adrian.blogweb1.model.Permission;
import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.repository.IPermissionRepository;
import com.adrian.blogweb1.repository.IRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

    @Service
    @RequiredArgsConstructor
    public class DatabaseInitializationService {

        private final IRoleRepository roleRepository;
        private final IPermissionRepository permissionRepository;
        private final UserService userService;

        /**
         * Ejecuta toda la lógica de inicialización de la base de datos
         * dentro de una única transacción para garantizar la consistencia.
         */
        @Transactional
        public void initializeDatabase() {
            // --- PASO 1: Crear Permisos ---
            createPermissionIfNotFound("READ");
            createPermissionIfNotFound("CREATE");
            createPermissionIfNotFound("UPDATE");
            createPermissionIfNotFound("DELETE");

            // --- PASO 2: Crear Roles ---
            createRoleIfNotFound("ROLE_USER", Set.of("READ"));
            createRoleIfNotFound("ROLE_ADMIN", Set.of("READ", "CREATE", "UPDATE", "DELETE"));

            // --- PASO 3: Crear Usuario por Defecto ---
            userService.createDefaultUser();
        }

        // Los métodos auxiliares ahora son privados dentro de este servicio.
        // No necesitan @Transactional porque el método público que los llama ya lo es.

        private void createPermissionIfNotFound(String name) {
            permissionRepository.findByPermissionName(name)
                    .orElseGet(() -> {
                        System.out.println(">>> Permiso '" + name + "' creado en el arranque.");
                        return permissionRepository.save(new Permission(name));
                    });
        }

        private void createRoleIfNotFound(String roleName, Set<String> permissionNames) {
            roleRepository.findByRole(roleName)
                    .ifPresentOrElse(
                            role -> {}, // Si existe, no hacer nada
                            () -> {
                                Role newRole = new Role();
                                newRole.setRole(roleName);

                                // Esta lógica ahora funciona porque todos los objetos
                                // viven en la misma transacción gestionada por initializeDatabase().
                                Set<Permission> permissions = permissionNames.stream()
                                        .map(name -> permissionRepository.findByPermissionName(name)
                                                .orElseThrow(() -> new IllegalStateException("Error de Configuración: Permiso " + name + " no encontrado.")))
                                        .collect(Collectors.toSet());

                                newRole.setPermissionsList(permissions);
                                roleRepository.save(newRole);
                                System.out.println(">>> Rol '" + roleName + "' creado con sus permisos.");
                            }
                    );
        }
    }


