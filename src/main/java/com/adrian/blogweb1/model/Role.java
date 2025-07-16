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
@Table(name= "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRole;
    private String role;

    @JsonIgnore  // Lado "no manejado" de la relación UserSec ↔ Role
    @ManyToMany(mappedBy = "rolesList")
    private Set<UserSec> users = new HashSet<>();
    // Usamos Set porque no permite repetidos
    // List permite repetidos

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable (name = "roles_permissions", joinColumns = @JoinColumn(name= "role_id"),
            inverseJoinColumns=@JoinColumn(name = "permission_id"))
    private Set<Permission> permissionsList = new HashSet<>();



}



