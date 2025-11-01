package com.adrian.blogweb1.dto;


import java.time.LocalDateTime;



public class PostResponseDTO {
    private Long idPost;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private String authorName; // Es más útil devolver el nombre que el objeto entero

    public PostResponseDTO() {
    }

    public PostResponseDTO(Long idPost, String title, String content, LocalDateTime createdAt, String authorName) {
        this.idPost = idPost;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.authorName = authorName;
    }

    public Long getIdPost() {
        return idPost;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setIdPost(Long idPost) {
        this.idPost = idPost;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }
}


