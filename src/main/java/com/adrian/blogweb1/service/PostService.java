package com.adrian.blogweb1.service;


import com.adrian.blogweb1.dto.PostCreateRequestDTO;
import com.adrian.blogweb1.dto.PostResponseDTO;
import com.adrian.blogweb1.dto.PostUpdateRequestDTO;
import com.adrian.blogweb1.model.Author;
import com.adrian.blogweb1.model.Post;
import com.adrian.blogweb1.repository.IAuthorRepository;
import com.adrian.blogweb1.repository.IPostRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PostService implements IPostService {


    private final IPostRepository postRepository;
    private final IAuthorRepository authorRepository;

    @Override
    public Post savePost(PostCreateRequestDTO postRequest) {
        // 1. Busca al autor por el ID proporcionado en el DTO.
        //    Si no lo encuentra, lanza una excepción.
        Author author = authorRepository.findById(postRequest.getAuthorId())
                .orElseThrow(() -> new RuntimeException("Autor no encontrado con id: " + postRequest.getAuthorId()));

        // 2. Crea una nueva entidad Post.
        Post newPost = new Post();
        newPost.setTitle(postRequest.getTitle());
        newPost.setContent(postRequest.getContent());
        newPost.setAuthor(author); // 3. Asocia el autor encontrado.

        // 4. Guarda el post. El método @PrePersist se encargará de poner la fecha.
        return postRepository.save(newPost);
    }


    @Override
    public List<PostResponseDTO> getPosts() {
        return postRepository.findAll()
                .stream()
                .map(this::mapToPostResponseDTO) // Mapeamos cada Post a un DTO
                .toList();
    }

    @Override
    public Optional<PostResponseDTO> getPostById(Long idPost) {
        return postRepository.findById(idPost)
                .map(this::mapToPostResponseDTO); // Mapeamos el Post si exist postRepository.findById(idPost).map(this::mapToPostResponseDTO);

    }

    // --- MÉTODO PRIVADO DE AYUDA ---
    // Este método centraliza la lógica de conversión de Entidad a DTO
    private PostResponseDTO mapToPostResponseDTO(Post post) {
        return new PostResponseDTO(
                post.getIdPost(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getAuthor().getName() // Asumiendo que Author tiene un método getName()
        );
    }

    @Override
    public PostResponseDTO updatePost(Long id, PostUpdateRequestDTO postDetails) {
        // 1. Busca el post existente o lanza una excepción si no lo encuentra
        Post postToUpdate = postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Post no encontrado con id: " + id));

        // 2. Actualiza los campos con la información del DTO
        postToUpdate.setTitle(postDetails.getTitle());
        postToUpdate.setContent(postDetails.getContent());

        // 3. Guarda los cambios
        Post updatedPost = postRepository.save(postToUpdate);

        // 4. Mapea la entidad actualizada a un DTO de respuesta
        return new PostResponseDTO(
                updatedPost.getIdPost(),
                updatedPost.getTitle(),
                updatedPost.getContent(),
                updatedPost.getCreatedAt(),
                updatedPost.getAuthor().getName()
        );
    }


    @Override
    public void deletePost(Long idPost) {
        postRepository.deleteById(idPost);
    }

}
