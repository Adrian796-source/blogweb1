package com.adrian.blogweb1.service;

import com.adrian.blogweb1.model.Permission;
import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.repository.IPermissionRepository;
import com.adrian.blogweb1.repository.IRoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DatabaseInitializationService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializationService.class);

    private final IRoleRepository roleRepository;
    private final IPermissionRepository permissionRepository;
    private final UserService userService;

    @Transactional
    public void initializeDatabase() {
        createPermissionIfNotFound("READ");
        createPermissionIfNotFound("CREATE");
        createPermissionIfNotFound("UPDATE");
        createPermissionIfNotFound("DELETE");

        createRoleIfNotFound("ROLE_USER", Set.of("READ"));
        createRoleIfNotFound("ROLE_ADMIN", Set.of("READ", "CREATE", "UPDATE", "DELETE"));

        userService.createDefaultUser();
    }

    private void createPermissionIfNotFound(String name) {
        permissionRepository.findByPermissionName(name)
                .ifPresentOrElse(
                        permission -> {},
                        () -> {
                            log.info(">>> Permiso '{}' creado en el arranque.", name);
                            permissionRepository.save(new Permission(name));
                        }
                );
    }

    private void createRoleIfNotFound(String roleName, Set<String> permissionNames) {
        roleRepository.findByRole(roleName)
                .ifPresentOrElse(
                        role -> {}, 
                        () -> {
                            Role newRole = new Role();
                            newRole.setRole(roleName);

                            Set<Permission> permissions = permissionNames.stream()
                                    .map(name -> permissionRepository.findByPermissionName(name)
                                            .orElseThrow(() -> new IllegalStateException("Error de Configuración: Permiso " + name + " no encontrado.")))
                                    .collect(Collectors.toSet());

                            newRole.setPermissionsList(permissions);
                            roleRepository.save(newRole);
                            log.info(">>> Rol '{}' creado con sus permisos.", roleName);
                        }
                );
    }
}
