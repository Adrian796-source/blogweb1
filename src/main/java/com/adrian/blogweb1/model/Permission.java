package com.adrian.blogweb1.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;


    @Entity
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Table(name="permissions")
    public class Permission {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long idPermission;
        @Column(unique = true, nullable = false)
        private String permissionName;

        @JsonIgnore
        @ManyToMany(mappedBy = "permissionsList", fetch = FetchType.LAZY)
        private Set<Role> rolesList = new HashSet<>();


        // Constructor de conveniencia para crear un permiso solo con su nombre.
        // Es muy útil para cuando creamos nuevos permisos que aún no tienen ID.
        public Permission(String permissionName) {
            this.permissionName = permissionName;
        }

    }







