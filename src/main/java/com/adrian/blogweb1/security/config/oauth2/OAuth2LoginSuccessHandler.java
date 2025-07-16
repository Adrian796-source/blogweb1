package com.adrian.blogweb1.security.config.oauth2;

import com.adrian.blogweb1.utils.JwtUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2LoginSuccessHandler.class);

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    // Se elimina IUserService porque no se usa directamente aquí.
    public OAuth2LoginSuccessHandler(JwtUtils jwtUtils, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            logger.info("--- [OAuth2LoginSuccessHandler] - INICIO. Autenticación OAuth2 exitosa.");

            CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
            String email = oAuth2User.getEmail();
            String username = oAuth2User.getUsername();

            if (username == null) {
                throw new IllegalStateException("El username obtenido de GitHub es nulo. No se puede continuar.");
            }

            logger.info("Datos de GitHub -> Email: '{}', Username: '{}'", email, username);

            logger.info("Paso 1: Intentando cargar UserDetails para el username: '{}'", username);
            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserByUsername(username);
                logger.info("Paso 1: ÉXITO. UserDetails cargado. Roles: {}", userDetails.getAuthorities());
            } catch (UsernameNotFoundException e) {
                logger.error("Paso 1: FALLO. UserDetailsService no pudo encontrar al usuario '{}'.", username, e);
                throw e; // Relanzamos la excepción para que el catch principal la maneje.
            }

            logger.info("Paso 2: Creando la autenticación para el token JWT.");
            Authentication jwtAuthentication = new UsernamePasswordAuthenticationToken(
                    userDetails.getUsername(),
                    null,
                    userDetails.getAuthorities()
            );
            logger.info("Paso 2: ÉXITO. Objeto de autenticación para JWT creado.");

            logger.info("Paso 3: Creando el token JWT.");
            String token = jwtUtils.createToken(jwtAuthentication);
            logger.info("Paso 3: ÉXITO. Token JWT generado.");

            logger.info("Paso 4: Enviando respuesta JSON al cliente.");
            response.setContentType("application/json");
            response.getWriter().write(
                    String.format("{\"token\":\"Bearer %s\", \"email\":\"%s\", \"username\":\"%s\"}", token, email, username)
            );
            logger.info("--- [OAuth2LoginSuccessHandler] - FIN. Respuesta enviada exitosamente.");

        } catch (Exception e) {
            logger.error("!!!!!!!! ERROR FATAL DENTRO DE OAuth2LoginSuccessHandler !!!!!!!!", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Ocurrió un error interno al procesar el login de OAuth2. Revise los logs del servidor.\", \"message\":\"" + e.getMessage() + "\"}");
        }
    }
}
