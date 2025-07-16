package com.adrian.blogweb1.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthorCreateRequestDTO {

    @NotBlank(message = "El nombre del autor no puede estar vacío")
    private String name;
}