package com.adrian.blogweb1.service;

import com.adrian.blogweb1.dto.AuthorCreateRequestDTO;
import com.adrian.blogweb1.dto.AuthorDTO;
import com.adrian.blogweb1.dto.AuthorUpdateRequestDTO;
import com.adrian.blogweb1.model.Author;

import java.util.List;
import java.util.Optional;

public interface IAuthorService {

    List<AuthorDTO> getAuthorsDTO();
    Optional<AuthorDTO> getAuthorByIdDTO(Long id);
    AuthorDTO createAuthor(AuthorCreateRequestDTO authorRequest);
    AuthorDTO updateAuthor(Long id, AuthorUpdateRequestDTO authorRequest);
    void deleteAuthor(Long id);
}