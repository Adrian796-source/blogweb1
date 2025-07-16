package com.adrian.blogweb1.service;


import com.adrian.blogweb1.model.Permission;

import java.util.List;
import java.util.Optional;

public interface IPermissionService {

    List<Permission> findAll();
    Optional<Permission> findById(Long id);
    Permission save(Permission permission);
    Permission updatePermission(Long idPermission, Permission permissionDetails);
    void deletePermission(Long idPermission);

}

