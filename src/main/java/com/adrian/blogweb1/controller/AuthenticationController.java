package com.adrian.blogweb1.controller;

import com.adrian.blogweb1.dto.AuthLoginRequestDTO;
import com.adrian.blogweb1.dto.AuthResponseDTO;
import com.adrian.blogweb1.utils.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller; // <-- CAMBIO 1
import org.springframework.web.bind.annotation.*;

@Controller // <-- CAMBIO 1: De @RestController a @Controller para más flexibilidad
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    /**
     * Endpoint para autenticar usuarios con nombre de usuario y contraseña.
     * Devuelve un JSON con el token JWT.
     */
    @PostMapping("/login")
    @ResponseBody // <-- CAMBIO 2: Asegura que este método SIGUE devolviendo JSON
    public ResponseEntity<AuthResponseDTO> login(@RequestBody @Valid AuthLoginRequestDTO authLoginRequest) {
        // --- NINGÚN CAMBIO EN ESTA LÓGICA ---
        UsernamePasswordAuthenticationToken loginToken = new UsernamePasswordAuthenticationToken(
                authLoginRequest.username(),
                authLoginRequest.password()
        );

        Authentication authentication = this.authenticationManager.authenticate(loginToken);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwt = this.jwtUtils.createToken(authentication);

        AuthResponseDTO response = new AuthResponseDTO(
                userDetails.getUsername(),
                "User logged in successfully",
                jwt,
                true
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * CAMBIO 3: Nuevo endpoint para iniciar el flujo de login con GitHub.
     * No devuelve JSON, sino que redirige al usuario.
     */
    @GetMapping("/login-oauth")
    public String oauthLogin() {
        // Redirige a la URL de autorización de Spring Security para el proveedor "github"
        return "redirect:/oauth2/authorization/github";
    }
}