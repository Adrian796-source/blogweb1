package com.adrian.blogweb1.model;



import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name="users")
@JsonIgnoreProperties({"authorities", "accountNonExpired", "accountNonLocked", "credentialsNonExpired"})
public class UserSec implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user_sec")
    private Long idUserSec;
    @Column(unique = true)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String username;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    private boolean enabled = true;
    private boolean accountNotExpired = true;
    private boolean accountNotLocked = true;
    private boolean credentialNotExpired = true;
    @Column(unique = true, nullable = true) // nullable para usuarios sociales sin email
    private String email;


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> rolesList = new HashSet<>();

    /**
     * Devuelve las autoridades concedidas al usuario.
     * Combina los roles (con prefijo "ROLE_") y los permisos únicos de esos roles.
     * Este método es requerido por la interfaz UserDetails.
     */
    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        // 1. Mapea cada rol a una autoridad (ej. "ROLE_ADMIN")
        Stream<GrantedAuthority> roleAuthorities = this.rolesList.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRole()));

        // 2. Mapea cada permiso de cada rol a una autoridad (ej. "CREATE", "READ")
        Stream<GrantedAuthority> permissionAuthorities = this.rolesList.stream()
                .flatMap(role -> role.getPermissionsList().stream())
                .map(permission -> new SimpleGrantedAuthority(permission.getPermissionName()));

        // 3. Concatena ambos streams y los recolecta en una lista
        return Stream.concat(roleAuthorities, permissionAuthorities)
                .collect(Collectors.toList());
    }

}
