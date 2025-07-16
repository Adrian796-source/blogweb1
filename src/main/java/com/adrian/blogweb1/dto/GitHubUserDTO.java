// En un nuevo archivo: GitHubUserDTO.java
package com.adrian.blogweb1.dto;

// Mapeamos solo los campos que nos interesan de la respuesta de GitHub
public record GitHubUserDTO(
        Long id,
        String login, // Este es el 'username' en GitHub
        String name,
        String email
) {}
