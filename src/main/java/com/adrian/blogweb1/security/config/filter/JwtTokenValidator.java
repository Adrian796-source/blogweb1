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
import java.util.stream.Stream;

public class JwtTokenValidator extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    // CORRECCIÓN 1: Renombramos el logger para evitar el "shadowing" con la clase padre.
    private static final Logger log = LoggerFactory.getLogger(JwtTokenValidator.class);

    public JwtTokenValidator(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(@Nonnull HttpServletRequest request,
                                    @Nonnull HttpServletResponse response,
                                    @Nonnull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwtToken = authHeader.substring(7);
            DecodedJWT decodedJWT = jwtUtils.validateToken(jwtToken);

            String username = jwtUtils.extractUsername(decodedJWT);

            List<String> roles = decodedJWT.getClaim("roles").asList(String.class);
            List<String> permissions = decodedJWT.getClaim("permissions").asList(String.class);

            Stream<GrantedAuthority> roleAuthorities = (roles != null) ?
                    roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)) : Stream.empty();

            Stream<GrantedAuthority> permissionAuthorities = (permissions != null) ?
                    permissions.stream().map(SimpleGrantedAuthority::new) : Stream.empty();

            // CORRECCIÓN 2: Usamos el método moderno .toList() de Java 16+.
            List<GrantedAuthority> authorities = Stream.concat(roleAuthorities, permissionAuthorities)
                    .toList();

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    username,
                    null, 
                    authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JWTVerificationException e) {
            // Usamos el logger con el nuevo nombre 'log'.
            log.error("Error de validación de token: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\": \"Token inválido o expirado\", \"status\": 401}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}