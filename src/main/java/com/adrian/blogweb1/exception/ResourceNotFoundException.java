package com.adrian.blogweb1.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

    @ResponseStatus(HttpStatus.NOT_FOUND) // Retorna HTTP 404 automáticamente
    public class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message); // Llama al constructor de RuntimeException
        }
    }


