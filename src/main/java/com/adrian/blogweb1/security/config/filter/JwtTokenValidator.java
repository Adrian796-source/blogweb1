package com.adrian.blogweb1.security.config.filter;


import com.adrian.blogweb1.utils.JwtUtils;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class JwtTokenValidator extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    // Añadimos un logger para registrar errores de validación
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenValidator.class);

    public JwtTokenValidator(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
                                    @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Si no hay token, simplemente continuamos con la cadena de filtros.
            // Spring Security se encargará de denegar el acceso si el endpoint es protegido.
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwtToken = authHeader.substring(7);
            DecodedJWT decodedJWT = jwtUtils.validateToken(jwtToken);

            String username = jwtUtils.extractUsername(decodedJWT);

            // Sugerencia: Usar Streams para construir las autoridades de forma más elegante
            List<String> roles = decodedJWT.getClaim("roles").asList(String.class);
            List<String> permissions = decodedJWT.getClaim("permissions").asList(String.class);

            Stream<GrantedAuthority> roleAuthorities = (roles != null) ?
                    roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)) : Stream.empty();

            Stream<GrantedAuthority> permissionAuthorities = (permissions != null) ?
                    permissions.stream().map(SimpleGrantedAuthority::new) : Stream.empty();

            List<GrantedAuthority> authorities = Stream.concat(roleAuthorities, permissionAuthorities)
                    .collect(Collectors.toList());

            // Crear el objeto de autenticación
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    username,
                    null, // Las credenciales no son necesarias después de la autenticación por token
                    authorities
            );

            // Establecer la autenticación en el contexto de seguridad de Spring
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JWTVerificationException e) {
            // Sugerencia: Capturar la excepción específica y dar una respuesta JSON
            logger.error("Error de validación de token: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401 es más apropiado
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\": \"Token inválido o expirado\", \"status\": 401}");
            return; // Detenemos la cadena de filtros aquí
        }

        filterChain.doFilter(request, response);
    }
}