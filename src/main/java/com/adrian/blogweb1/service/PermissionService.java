package com.adrian.blogweb1.service;

import com.adrian.blogweb1.exception.ResourceNotFoundException;
import com.adrian.blogweb1.model.Permission;
import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.repository.IPermissionRepository;
import com.adrian.blogweb1.repository.IRoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PermissionService implements IPermissionService{



    private final IRoleRepository roleRepository;
    private final IPermissionRepository permissionRepository;


    @Override
    public List<Permission> findAll() {

        return permissionRepository.findAll();
    }

    @Override
    public Optional<Permission> findById(Long id) {

        return permissionRepository.findById(id);
    }

    @Override
    public Permission save(Permission permission) {
        return permissionRepository.save(permission);
    }

    @Override
    @Transactional
    public Permission updatePermission(Long idPermission, Permission permissionDetails) {
        Permission permission = permissionRepository.findById(idPermission)
                .orElseThrow(() -> new ResourceNotFoundException("Permiso no encontrado con id: " + idPermission));

        // Actualización correcta usando permissionName (como está en tu entidad)
        if (permissionDetails.getPermissionName() != null) {
            permission.setPermissionName(permissionDetails.getPermissionName());
        }

        return permissionRepository.save(permission);
    }

    @Override
    @Transactional
    public void deletePermission(Long idPermission) {
        Permission permission = permissionRepository.findById(idPermission)
                .orElseThrow(() -> new ResourceNotFoundException("Permiso no encontrado con id: " + idPermission));

        // 1. Obtener todos los roles que tienen este permiso
        List<Role> rolesWithPermission = roleRepository.findRolesByPermissionId(idPermission);

        // 2. Remover el permiso de cada rol
        rolesWithPermission.forEach(role ->
                role.getPermissionsList().removeIf(p -> p.getIdPermission().equals(idPermission))
        );

        // 3. Guardar los roles actualizados
        roleRepository.saveAll(rolesWithPermission);

        // 4. Finalmente eliminar el permiso
        permissionRepository.delete(permission);
    }




}




