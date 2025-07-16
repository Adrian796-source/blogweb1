package com.adrian.blogweb1.service;

import com.adrian.blogweb1.exception.ResourceNotFoundException;
import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.model.UserSec;
import com.adrian.blogweb1.repository.IRoleRepository;
import com.adrian.blogweb1.repository.IUserRepository;
import com.adrian.blogweb1.security.config.props.DefaultAdminProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // SUGERENCIA: Inyección por constructor con Lombok. ¡Más limpio y seguro!
public class UserService implements IUserService {

    // Las dependencias ahora son 'final' gracias a la inyección por constructor
    private final IUserRepository userRepository;
    private final IRoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final Environment env;
    private final IRoleRepository roleRepository;
    private final DefaultAdminProperties adminProperties;


    /**
     * Crea un usuario administrador por defecto si no existe.
     * Este método ASUME que los roles y permisos necesarios ya han sido creados
     * por un CommandLineRunner al arrancar la aplicación.
     */

    @Override
    @Transactional(readOnly = true) // Optimización para consultas
    public List<UserSec> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserSec> findById(Long idUserSec) {
        // SUGERENCIA: Mucho más simple. El repositorio ya devuelve un Optional.
        return userRepository.findById(idUserSec);
    }

    @Override
    @Transactional
    public UserSec save(UserSec userSec) {
        // Antes de guardar, asegurémonos de que la contraseña esté encriptada
        userSec.setPassword(passwordEncoder.encode(userSec.getPassword()));
        return userRepository.save(userSec);
    }

    @Override
    @Transactional
    public UserSec updateUser(Long idUserSec, UserSec userSecDetails) {
        UserSec existingUser = userRepository.findById(idUserSec)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + idUserSec));

        // Actualizar campos básicos
        existingUser.setUsername(userSecDetails.getUsername());
        existingUser.setEmail(userSecDetails.getEmail()); // Asumo que también quieres actualizar el email

        // Actualizar contraseña solo si se proporciona una nueva
        if (userSecDetails.getPassword() != null && !userSecDetails.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(userSecDetails.getPassword()));
        }

        // Actualizar estados
        existingUser.setEnabled(userSecDetails.isEnabled());
        existingUser.setAccountNotExpired(userSecDetails.isAccountNotExpired());
        existingUser.setAccountNotLocked(userSecDetails.isAccountNotLocked());
        existingUser.setCredentialNotExpired(userSecDetails.isCredentialNotExpired());

        // SUGERENCIA: Optimización para actualizar roles
        if (userSecDetails.getRolesList() != null && !userSecDetails.getRolesList().isEmpty()) {
            Set<Long> roleIds = userSecDetails.getRolesList().stream()
                    .map(Role::getIdRole)
                    .collect(Collectors.toSet());
            // Hacemos una sola consulta a la BD para traer todos los roles necesarios
            Set<Role> updatedRoles = roleService.findAllByIds(roleIds);
            existingUser.setRolesList(updatedRoles);
        }

        return userRepository.save(existingUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long idUserSec) {
        UserSec user = this.findById(idUserSec)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + idUserSec));

        // Romper la relación con los roles antes de eliminar
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
    public UserSec findOrCreateUserForOAuth(String email, String username) {
        // 1. Intenta encontrar al usuario por su email
        return this.findByEmail(email)
                .orElseGet(() -> {
                    // 2. Si no existe, lo crea

                    // Busca el rol por defecto para los nuevos usuarios.
                    // Es una buena práctica que este rol ya exista en la BD.
                    Role userRole = roleService.findByRoleName("ROLE_USER")
                            .orElseThrow(() -> new IllegalStateException("El rol por defecto 'ROLE_USER' no fue encontrado."));

                    // Crea la nueva entidad de usuario
                    UserSec newUser = new UserSec();
                    newUser.setEmail(email);
                    newUser.setUsername(username);
                    // Asignamos una contraseña aleatoria y segura, ya que no la usarán para login
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
        // passwordEncoder hace el trabajo real
        return passwordEncoder.encode(password);
    }

    // BORRA ESTE BLOQUE DE SecurityConfig.java
    /**
     * Crea el usuario administrador por defecto si no existe.
     * Este método ASUME que los roles y permisos necesarios ya fueron creados
     * por el CommandLineRunner en SecurityConfig.
     */
    @Transactional
    public void createDefaultUser() {
        String adminEmail = adminProperties.getEmail();

        // Solo proceder si el usuario admin no existe
        if (userRepository.findByEmail(adminEmail).isEmpty()) {

            // 1. BUSCAR el rol 'ROLE_ADMIN'. No crearlo.
            //    Lanza una excepción si no lo encuentra, porque es un error crítico.
            Role adminRole = roleRepository.findByRole("ROLE_ADMIN")
                    .orElseThrow(() -> new IllegalStateException(
                            "Error de configuración CRÍTICO: El rol 'ROLE_ADMIN' no fue encontrado al crear el usuario por defecto."
                    ));

            // 2. Crear la nueva entidad de usuario
            UserSec adminUser = new UserSec();
            adminUser.setUsername(adminProperties.getUsername());
            adminUser.setEmail(adminProperties.getEmail());
            adminUser.setPassword(passwordEncoder.encode(adminProperties.getPassword()));
            adminUser.setEnabled(true);
            adminUser.setAccountNotExpired(true);
            adminUser.setCredentialNotExpired(true);
            adminUser.setAccountNotLocked(true);

            // 3. Asignar el rol ENCONTRADO y GESTIONADO
            adminUser.setRolesList(Set.of(adminRole));

            // 4. Guardar el usuario.
            userRepository.save(adminUser);
            System.out.println(">>> Usuario administrador por defecto creado.");
        }
    }

}