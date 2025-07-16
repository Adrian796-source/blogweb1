package com.adrian.blogweb1.service;

import com.adrian.blogweb1.exception.ResourceNotFoundException;
import com.adrian.blogweb1.model.Permission;
import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.model.UserSec;
import com.adrian.blogweb1.repository.IPermissionRepository;
import com.adrian.blogweb1.repository.IRoleRepository;
import com.adrian.blogweb1.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RoleService implements IRoleService {


    private final IRoleRepository roleRepository;


    private final IUserRepository userRepository;


    private final IPermissionRepository permissionRepository;

    @Override
    public List<Role> findAll() {

        return roleRepository.findAll();
    }

    @Override
    public Optional<Role> findById(Long id) {

        return roleRepository.findById(id);
    }

    @Override
    public Role save(Role role) {

        return roleRepository.save(role);
    }

    @Override
    @Transactional
    public Role updateRolePermissions(Long idRole, Set<Permission> permissions) {
        // Buscar el rol por ID
        Role role = roleRepository.findById(idRole)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado con ID: " + idRole));

        // Limpiar los permisos actuales del rol
        role.getPermissionsList().clear();

        // Agregar los nuevos permisos al rol
        for (Permission permission : permissions) {
            Permission existingPermission = permissionRepository.findById(permission.getIdPermission())
                    .orElseThrow(() -> {
                        System.out.println("Permiso no encontrado con ID: " + permission.getIdPermission()); // Log para depuración
                        return new RuntimeException("Permiso no encontrado con ID: " + permission.getIdPermission());
                    });
            role.getPermissionsList().add(existingPermission);
        }

        // Guardar el rol actualizado
        return roleRepository.save(role);
    }

    @Override
    @Transactional
    public void deleteRole(Long idRole) {
        Role role = roleRepository.findById(idRole)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado"));

        // Romper relaciones con usuarios primero
        List<UserSec> usersWithRole = userRepository.findByRolesListContains(role);
        usersWithRole.forEach(user -> user.getRolesList().remove(role));
        userRepository.saveAll(usersWithRole);

        // Eliminar el rol
        roleRepository.delete(role);
    }

    @Override
    public Optional<Role> findByRoleName(String roleUser) {
        return roleRepository.findByRole(roleUser);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Role> findAllByIds(Set<Long> roleIds) {
        // JpaRepository ya tiene un método findAllById que es perfecto para esto.
        // Devuelve una List, así que la convertimos a un Set.
        return new HashSet<>(roleRepository.findAllById(roleIds));
    }

}




