package com.adrian.blogweb1.service;

import com.adrian.blogweb1.exception.ResourceNotFoundException;
import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.model.UserSec;
import com.adrian.blogweb1.repository.IRoleRepository;
import com.adrian.blogweb1.repository.IUserRepository;
import com.adrian.blogweb1.security.config.props.DefaultAdminProperties;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements IUserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final IUserRepository userRepository;
    private final IRoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final Environment env;
    private final IRoleRepository roleRepository;
    private final DefaultAdminProperties adminProperties;

    // --- INICIO DE LA SOLUCIÓN ---
    // Campo para la auto-inyección del proxy del servicio.
    private IUserService userService;

    // Usamos inyección por setter con @Lazy para romper la dependencia circular de forma segura.
    @Autowired
    public void setUserService(@Lazy IUserService userService) {
        this.userService = userService;
    }
    // --- FIN DE LA SOLUCIÓN ---

    @Override
    @Transactional(readOnly = true)
    public List<UserSec> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSec> findById(Long idUserSec) {
        return userRepository.findById(idUserSec);
    }

    @Override
    @Transactional
    public UserSec save(UserSec userSec) {
        userSec.setPassword(passwordEncoder.encode(userSec.getPassword()));
        return userRepository.save(userSec);
    }

    @Override
    @Transactional
    public UserSec updateUser(Long idUserSec, UserSec userSecDetails) {
        UserSec existingUser = userRepository.findById(idUserSec)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + idUserSec));

        existingUser.setUsername(userSecDetails.getUsername());
        existingUser.setEmail(userSecDetails.getEmail());

        if (userSecDetails.getPassword() != null && !userSecDetails.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(userSecDetails.getPassword()));
        }

        existingUser.setEnabled(userSecDetails.isEnabled());
        existingUser.setAccountNotExpired(userSecDetails.isAccountNotExpired());
        existingUser.setAccountNotLocked(userSecDetails.isAccountNotLocked());
        existingUser.setCredentialNotExpired(userSecDetails.isCredentialNotExpired());

        if (userSecDetails.getRolesList() != null && !userSecDetails.getRolesList().isEmpty()) {
            Set<Long> roleIds = userSecDetails.getRolesList().stream()
                    .map(Role::getIdRole)
                    .collect(Collectors.toSet());
            Set<Role> updatedRoles = roleService.findAllByIds(roleIds);
            existingUser.setRolesList(updatedRoles);
        }

        return userRepository.save(existingUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long idUserSec) {
        // La llamada a través de la dependencia inyectada asegura que se use el proxy
        // transaccional, solucionando el "code smell" de Sonar.
        UserSec user = userService.findById(idUserSec)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + idUserSec));

        user.getRolesList().clear();
        userRepository.save(user);

        userRepository.delete(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSec> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    @Transactional
    public UserSec findOrCreateUserForOAuth(String email, String username) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    Role userRole = roleService.findByRoleName("ROLE_USER")
                            .orElseThrow(() -> new IllegalStateException("El rol por defecto 'ROLE_USER' no fue encontrado."));

                    UserSec newUser = new UserSec();
                    newUser.setEmail(email);
                    newUser.setUsername(username);
                    newUser.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
                    newUser.setEnabled(true);
                    newUser.setAccountNotExpired(true);
                    newUser.setCredentialNotExpired(true);
                    newUser.setAccountNotLocked(true);
                    newUser.setRolesList(Set.of(userRole));

                    return userRepository.save(newUser);
                });
    }

    @Override
    public String encryptPassword(String password) {
        return passwordEncoder.encode(password);
    }

    @Transactional
    public void createDefaultUser() {
        String adminEmail = adminProperties.getEmail();

        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            Role adminRole = roleRepository.findByRole("ROLE_ADMIN")
                    .orElseThrow(() -> new IllegalStateException(
                            "Error de configuración CRÍTICO: El rol 'ROLE_ADMIN' no fue encontrado al crear el usuario por defecto."
                    ));

            UserSec adminUser = new UserSec();
            adminUser.setUsername(adminProperties.getUsername());
            adminUser.setEmail(adminProperties.getEmail());
            adminUser.setPassword(passwordEncoder.encode(adminProperties.getPassword()));
            adminUser.setEnabled(true);
            adminUser.setAccountNotExpired(true);
            adminUser.setCredentialNotExpired(true);
            adminUser.setAccountNotLocked(true);

            adminUser.setRolesList(Set.of(adminRole));

            userRepository.save(adminUser);
            log.info(">>> Usuario administrador por defecto creado.");
        }
    }

}