package com.adrian.blogweb1.security.config;

import com.adrian.blogweb1.security.config.props.DefaultAdminProperties;
import com.adrian.blogweb1.service.DatabaseInitializationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.adrian.blogweb1.security.config.filter.JwtTokenValidator;
import com.adrian.blogweb1.service.UserDetailsServiceImp;
import com.adrian.blogweb1.utils.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // --- INICIO DE LA CORRECCIÓN ---
                // Habilitamos CSRF y lo configuramos para que funcione con APIs stateless.
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()) //NOSONAR
                )
                // --- FIN DE LA CORRECCIÓN ---
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/auth/login", "/error").permitAll();
                    auth.requestMatchers("/auth/login-oauth", "/oauth2/**", "/login/oauth2/code/**").permitAll();
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(new JwtTokenValidator(jwtUtils), UsernamePasswordAuthenticationFilter.class)
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
                // ¡AQUÍ ESTÁ LA MEJORA!
                // Le decimos a Spring Security cómo manejar los errores de autenticación para una API REST.
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Configuration
    @Profile("!test") // Le dice a Spring: "No cargues esta configuración si el perfil 'test' está activo"
    public static class DefaultUserConfig {

        private static final Logger logger = LoggerFactory.getLogger(DefaultUserConfig.class);

        @Bean
        public CommandLineRunner initializeDatabase(DatabaseInitializationService initializationService) {
            return args -> {
                logger.info(">>> Iniciando la carga de datos por defecto...");
                initializationService.initializeDatabase();
                logger.info(">>> Carga de datos por defecto completada.");
            };
        }

        @Bean
        public WebClient.Builder webClientBuilder() {
            return WebClient.builder();
        }
    }
}