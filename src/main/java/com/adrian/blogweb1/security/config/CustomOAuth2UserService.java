package com.adrian.blogweb1.security.config;


import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.model.UserSec;
import com.adrian.blogweb1.repository.IRoleRepository;
import com.adrian.blogweb1.repository.IUserRepository;
import com.adrian.blogweb1.security.config.props.DefaultAdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final DefaultAdminProperties adminProperties;

    public CustomOAuth2UserService(IUserRepository userRepository,
                                   IRoleRepository roleRepository,
                                   PasswordEncoder passwordEncoder,
                                   DefaultAdminProperties adminProperties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminProperties = adminProperties;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();

        UserSec userSec = findOrCreateUser(oauthUser);

        Collection<? extends GrantedAuthority> authorities = userSec.getAuthorities();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        return new CustomOAuth2User(oauthUser, registrationId, userNameAttributeName, authorities);
    }

    UserSec findOrCreateUser(OAuth2User oauthUser) {
        String email = Optional.ofNullable(oauthUser.<String>getAttribute("email"))
                .orElseThrow(() -> new OAuth2AuthenticationException("No se pudo obtener el email de GitHub. Asegúrate de que sea público en tu perfil."));

        String username = Optional.ofNullable(oauthUser.<String>getAttribute("login"))
                .orElse(email.split("@")[0]);

        return userRepository.findByEmail(email)
                .map(existingUser -> {
                    log.info(">>> Usuario existente encontrado por email: {}", email);
                    existingUser.setUsername(username);
                    return checkAndApplyAdminRole(existingUser);
                })
                .orElseGet(() -> {
                    log.info(">>> No se encontró usuario por email: {}. Creando nuevo usuario...", email);
                    return createNewUser(email, username);
                });
    }

    private UserSec createNewUser(String email, String username) {
        UserSec newUser = new UserSec();
        newUser.setEmail(email);
        newUser.setUsername(username);
        newUser.setEnabled(true);
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        newUser.setAccountNotExpired(true);
        newUser.setAccountNotLocked(true);
        newUser.setCredentialNotExpired(true);
        return checkAndApplyAdminRole(newUser);
    }

    private UserSec checkAndApplyAdminRole(UserSec user) {
        String adminEmail = adminProperties.getEmail();
        String adminUsername = adminProperties.getUsername();

        boolean isAdmin = (adminEmail != null && adminEmail.equalsIgnoreCase(user.getEmail())) ||
                (adminUsername != null && adminUsername.equalsIgnoreCase(user.getUsername()));

        String roleName = isAdmin ? "ROLE_ADMIN" : "ROLE_USER";

        boolean hasCorrectRole = user.getRolesList() != null && user.getRolesList().stream()
                .anyMatch(role -> role.getRole().equals(roleName));

        if (!hasCorrectRole) {
            log.info(">>> Asignando/Actualizando rol para {} a: {}", user.getEmail(), roleName);
            Role assignedRole = roleRepository.findByRole(roleName)
                    .orElseThrow(() -> new IllegalStateException("Error crítico: Rol '" + roleName + "' no encontrado."));
            user.setRolesList(Set.of(assignedRole));
        }

        return userRepository.save(user);
    }
}
