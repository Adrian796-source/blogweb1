package com.adrian.blogweb1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthorDTO {
    private Long idAuthor;
    private String name;
    //No incluimos la lista de posts aquí.
    // Solo enviamos la información esencial del autor.
}
