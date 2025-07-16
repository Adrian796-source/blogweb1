package com.adrian.blogweb1.controller;

import com.adrian.blogweb1.dto.PostCreateRequestDTO;
import com.adrian.blogweb1.dto.PostResponseDTO;
import com.adrian.blogweb1.dto.PostUpdateRequestDTO;
import com.adrian.blogweb1.model.Post;
import com.adrian.blogweb1.service.IPostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final IPostService  postService;

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE')")
    public ResponseEntity<Post> createPost(@RequestBody @Valid PostCreateRequestDTO postRequest) {
        // 1. Ahora el método recibe el DTO, que es más seguro y limpio.
        // 2. Se lo pasamos al servicio, que ya sabe cómo manejarlo.
        Post createdPost = postService.savePost(postRequest);

        // 3. Devolvemos 201 Created, que es el estándar para la creación de recursos.
        return new ResponseEntity<>(createdPost, HttpStatus.CREATED);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<List<PostResponseDTO>> getPosts() {
        return ResponseEntity.ok(postService.getPosts());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('READ')")
    public ResponseEntity<PostResponseDTO> getPostById(@PathVariable Long id) {
        return postService.getPostById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


    @PutMapping("/{id}")
    // Lógica de seguridad avanzada: Pasa si eres ADMIN o si eres el autor del post.
    @PreAuthorize("hasAuthority('UPDATE')")
    public ResponseEntity<PostResponseDTO> updatePost(
            @PathVariable Long id,
            @RequestBody @Valid PostUpdateRequestDTO postDetails // Usamos el DTO de actualización
    ) {
        // El controlador es más limpio sin try-catch. La gestión de excepciones
        // se debe hacer de forma global con @ControllerAdvice.
        PostResponseDTO updatedPost = postService.updatePost(id, postDetails);
        return ResponseEntity.ok(updatedPost);
    }

    @DeleteMapping("/{id}")
    // Aplicamos la misma lógica de seguridad que en el update.
    @PreAuthorize("hasAuthority('DELETE')")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) { // Estandarizamos a 'id'
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
}

