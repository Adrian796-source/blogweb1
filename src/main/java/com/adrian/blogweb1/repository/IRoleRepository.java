package com.adrian.blogweb1.repository;



import com.adrian.blogweb1.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IRoleRepository extends JpaRepository<Role, Long> {

    // Método para encontrar roles por permiso (usando el nombre correcto del campo)
    @Query("SELECT r FROM Role r JOIN r.permissionsList p WHERE p.idPermission = :permissionId")
    List<Role> findRolesByPermissionId(@Param("permissionId") Long permissionId);

    Optional<Role> findByRole(String role);

    }


