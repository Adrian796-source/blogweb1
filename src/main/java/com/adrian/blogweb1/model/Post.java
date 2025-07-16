package com.adrian.blogweb1.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idPost;

    private String title;

    @Column(length = 2000)
    private String content;


    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "author_id", referencedColumnName = "idAuthor", nullable = false)
    @JsonBackReference
    private Author author;

    // El servidor genera automaticamente la fecha
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }


}
