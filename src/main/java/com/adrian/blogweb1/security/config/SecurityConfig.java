package com.adrian.blogweb1.security.config;

import com.adrian.blogweb1.security.config.props.DefaultAdminProperties;
import com.adrian.blogweb1.service.DatabaseInitializationService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.adrian.blogweb1.security.config.filter.JwtTokenValidator;
import com.adrian.blogweb1.security.config.oauth2.CustomOAuth2UserService;
import com.adrian.blogweb1.security.config.oauth2.OAuth2LoginSuccessHandler;
import com.adrian.blogweb1.service.UserDetailsServiceImp;
import com.adrian.blogweb1.service.UserService;
import com.adrian.blogweb1.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.reactive.function.client.WebClient;


@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor // Inyección de dependencias por constructor con Lombok
@EnableConfigurationProperties(DefaultAdminProperties.class)
public class SecurityConfig {

    // Dependencias inyectadas vía constructor (más seguro y recomendado)
    private final JwtUtils jwtUtils;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailsServiceImp userDetailsService;

    /**
     * PRIMERA CADENA DE FILTROS: Autenticación (OAuth2)
     * - Anotada con @Order(1) para que se evalúe primero.
     * - Se aplica SÓLO a las rutas de autenticación gracias a securityMatcher.
     * - Su propósito es autenticar al usuario (vía OAuth2) y generar un JWT.
     * - NO tiene el filtro de validación de JWT (JwtTokenValidator).
     */


    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                // La política de sesión principal es STATELESS para nuestra API. Es la regla general.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // 1. Definimos TODAS las rutas públicas que no necesitan ninguna autenticación.
                    auth.requestMatchers("/auth/login", "/error").permitAll();
                    auth.requestMatchers("/auth/login-oauth", "/oauth2/**", "/login/oauth2/code/**").permitAll();

                    // 2. Para TODAS las demás peticiones, exigimos que estén autenticadas.
                    auth.anyRequest().authenticated();
                })
                // 3. Añadimos nuestro filtro para validar el token JWT en cada petición protegida.
                .addFilterBefore(new JwtTokenValidator(jwtUtils), UsernamePasswordAuthenticationFilter.class)

                // 4. Configuramos el flujo de login de OAuth2. Spring lo gestionará de forma segura.
                .oauth2Login(oauth2 -> {
                    oauth2.authorizationEndpoint(authorization -> authorization
                            .baseUri("/oauth2/authorization")
                    );
                    oauth2.redirectionEndpoint(redirection -> redirection
                            .baseUri("/login/oauth2/code/*")
                    );
                    oauth2.userInfoEndpoint(userInfo -> userInfo
                            .userService(customOAuth2UserService)
                    );
                    oauth2.successHandler(oAuth2LoginSuccessHandler);
                })
                .build();
    }

    /**
     * Expone el AuthenticationManager de Spring Security como un Bean.
     * Spring lo configurará automáticamente con el UserDetailsService y PasswordEncoder correctos.
     * @param authenticationConfiguration La configuración de autenticación de Spring.
     * @return El AuthenticationManager configurado.
     * @throws Exception Si hay un error al obtener el manager.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Configuration
    public static class DefaultUserConfig {

        @Bean
        public CommandLineRunner initializeDatabase(DatabaseInitializationService initializationService) {
            return args -> {
                System.out.println(">>> Iniciando la carga de datos por defecto...");
                initializationService.initializeDatabase();
                System.out.println(">>> Carga de datos por defecto completada.");
            };
        }

        @Bean
        public WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }
}