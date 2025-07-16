package com.adrian.blogweb1.repository;

import com.adrian.blogweb1.model.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IPostRepository extends JpaRepository<Post, Long> {
    List<Post>findByAuthor_IdAuthor(Long idAuthor);
}
