package com.adrian.blogweb1.service;

import com.adrian.blogweb1.model.UserSec;
import com.adrian.blogweb1.repository.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor // Inyección por constructor
public class UserDetailsServiceImp implements UserDetailsService {

    private final IUserRepository userRepository;

    @Override
    @Transactional(readOnly = true) // readOnly = true es una optimización para consultas
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UserSec user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con el nombre: " + username));

        // Usamos Streams para una construcción más limpia y moderna de las autoridades
        Stream<SimpleGrantedAuthority> roleAuthorities = user.getRolesList().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRole()));

        Stream<SimpleGrantedAuthority> permissionAuthorities = user.getRolesList().stream()
                .flatMap(role -> role.getPermissionsList().stream())
                .map(permission -> new SimpleGrantedAuthority(permission.getPermissionName()));

        List<SimpleGrantedAuthority> authorities = Stream.concat(roleAuthorities, permissionAuthorities)
                .collect(Collectors.toList());

        return new User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                user.isAccountNotExpired(),
                user.isCredentialNotExpired(),
                user.isAccountNotLocked(),
                authorities
        );
    }
}