package com.adrian.blogweb1.service;

import com.adrian.blogweb1.dto.PostCreateRequestDTO;
import com.adrian.blogweb1.dto.PostResponseDTO;
import com.adrian.blogweb1.dto.PostUpdateRequestDTO;
import com.adrian.blogweb1.model.Post;

import java.util.List;
import java.util.Optional;

public interface IPostService {

    Post savePost(PostCreateRequestDTO postRequest);
    List<PostResponseDTO> getPosts();
    Optional<PostResponseDTO> getPostById(Long idPost);
    PostResponseDTO updatePost(Long idPost, PostUpdateRequestDTO postDetails);
    void deletePost(Long idPost);

}
