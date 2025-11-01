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
        Author author = authorRepository.findById(postRequest.getAuthorId())
                .orElseThrow(() -> new RuntimeException("Autor no encontrado con id: " + postRequest.getAuthorId()));

        Post newPost = new Post();
        newPost.setTitle(postRequest.getTitle());
        newPost.setContent(postRequest.getContent());
        newPost.setAuthor(author);

        return postRepository.save(newPost);
    }


    @Override
    public List<PostResponseDTO> getPosts() {
        return postRepository.findAll()
                .stream()
                .map(this::mapToPostResponseDTO)
                .toList();
    }

    @Override
    public Optional<PostResponseDTO> getPostById(Long idPost) {
        return postRepository.findById(idPost)
                .map(this::mapToPostResponseDTO);
    }

    private PostResponseDTO mapToPostResponseDTO(Post post) {
        return new PostResponseDTO(
                post.getIdPost(),
                post.getTitle(),
                post.getContent(),
                post.getCreatedAt(),
                post.getAuthor().getName()
        );
    }

    @Override
    public PostResponseDTO updatePost(Long id, PostUpdateRequestDTO postDetails) {
        Post postToUpdate = postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Post no encontrado con id: " + id));

        postToUpdate.setTitle(postDetails.getTitle());
        postToUpdate.setContent(postDetails.getContent());

        Post updatedPost = postRepository.save(postToUpdate);

        return new PostResponseDTO(
                updatedPost.getIdPost(),
                updatedPost.getTitle(),
                updatedPost.getContent(),
                updatedPost.getCreatedAt(),
                updatedPost.getAuthor().getName()
        );
    }

    @Override
    public void deletePost(Long id) {
        // MEJORA: Verificamos que el post existe antes de intentar borrarlo.
        if (!postRepository.existsById(id)) {
            throw new EntityNotFoundException("No se puede eliminar. Post no encontrado con ID: " + id);
        }
        postRepository.deleteById(id);
    }
}


