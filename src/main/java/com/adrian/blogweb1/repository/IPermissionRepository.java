package com.adrian.blogweb1.repository;


import com.adrian.blogweb1.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IPermissionRepository extends JpaRepository <Permission, Long> {

    // Spring Data JPA entenderá que debe crear una consulta para buscar un Permiso por su campo 'name'.
    Optional<Permission> findByPermissionName(String permissionName);


}
