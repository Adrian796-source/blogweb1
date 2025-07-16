package com.adrian.blogweb1.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostResponseDTO {
    private Long idPost;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private String authorName; // Es más útil devolver el nombre que el objeto entero
}


