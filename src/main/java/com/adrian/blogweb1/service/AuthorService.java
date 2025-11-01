package com.adrian.blogweb1.service;

import com.adrian.blogweb1.dto.AuthorCreateRequestDTO;
import com.adrian.blogweb1.dto.AuthorDTO;
import com.adrian.blogweb1.dto.AuthorUpdateRequestDTO;
import com.adrian.blogweb1.model.Author;
import com.adrian.blogweb1.repository.IAuthorRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthorService implements IAuthorService {

    private final IAuthorRepository authorRepository;

    @Override
    public AuthorDTO createAuthor(AuthorCreateRequestDTO authorRequest) {
        Author newAuthor = new Author();
        newAuthor.setName(authorRequest.getName());

        Author savedAuthor = authorRepository.save(newAuthor);

        return mapToAuthorDTO(savedAuthor);
    }

    @Override
    public List<AuthorDTO> getAuthorsDTO() {
        return authorRepository.findAll() // Obtiene la lista de entidades Author
                .stream()                 // se convierte en un stream
                .map(this::mapToAuthorDTO) // Reutilizamos el método de ayuda para el mapeo
                .toList();                // se convierte de nuevo a una lista
    }

    @Override
    public Optional<AuthorDTO> getAuthorByIdDTO(Long idAuthor) {
        return authorRepository.findById(idAuthor)
                .map(this::mapToAuthorDTO); // Reutilizamos el método de ayuda aquí también
    }

    @Override
    public AuthorDTO updateAuthor(Long id, AuthorUpdateRequestDTO authorRequest) {
        Author authorToUpdate = authorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Autor no encontrado con id: " + id));

        authorToUpdate.setName(authorRequest.getName());
        Author updatedAuthor = authorRepository.save(authorToUpdate);

        return mapToAuthorDTO(updatedAuthor);
    }

    @Override
    public void deleteAuthor(Long id) {
        // Buscamos el autor primero para asegurarnos de que existe antes de intentar borrarlo.
        Author authorToDelete = authorRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se puede eliminar. Autor no encontrado con id: " + id));
        authorRepository.delete(authorToDelete);
    }

    // Método de ayuda para no repetir código
    private AuthorDTO mapToAuthorDTO(Author author) {
        return new AuthorDTO(author.getIdAuthor(), author.getName());
    }
}