package com.adrian.blogweb1.service;

import com.adrian.blogweb1.model.Permission;
import com.adrian.blogweb1.model.Role;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface IRoleService {
    List<Role> findAll();

    Optional<Role> findById(Long id);

    Role save(Role role);

    Role updateRolePermissions(Long idRole, Set<Permission> permissions);

    void deleteRole(Long idRole);

    Optional<Role> findByRoleName(String roleUser);

    Set<Role> findAllByIds(Set<Long> roleIds);
}

