package com.adrian.blogweb1.exceptionTest;

import com.adrian.blogweb1.exception.GlobalExceptionHandler;
import com.adrian.blogweb1.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// --- Controlador Falso (Dummy) para lanzar excepciones a propósito ---
@RestController
class ExceptionTestController {
    @GetMapping("/test/resource-not-found")
    public void throwResourceNotFound() {
        throw new ResourceNotFoundException("Test resource not found");
    }

    @GetMapping("/test/access-denied")
    public void throwAccessDenied() {
        // Esta excepción es lanzada por Spring Security, pero la simulamos aquí para el test.
        throw new AccessDeniedException("Test access denied");
    }

    @GetMapping("/test/generic-error")
    public void throwGenericError() {
        throw new RuntimeException("Test generic error");
    }
}

// --- Clase de Test ---
// Le decimos a Spring que cargue nuestro GlobalExceptionHandler y el controlador falso.
@WebMvcTest(controllers = {GlobalExceptionHandler.class, ExceptionTestController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    // Aunque no lo usemos, @WebMvcTest requiere que los beans de servicio se mockeen.
    // Añade aquí los @MockBean para los servicios que tus controladores reales puedan necesitar.
    // Por ejemplo:
    // @MockBean private IUserService userService;

    @Test
    @DisplayName("Debería manejar ResourceNotFoundException y devolver 404 Not Found")
    @WithMockUser // Necesario para pasar el filtro de seguridad
    void handleResourceNotFoundException_ShouldReturn404() throws Exception {
        mockMvc.perform(get("/test/resource-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", is("Test resource not found")));
    }

    @Test
    @DisplayName("Debería manejar AccessDeniedException y devolver 403 Forbidden")
    @WithMockUser
    void handleAccessDeniedException_ShouldReturn403() throws Exception {
        mockMvc.perform(get("/test/access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", is("Acceso denegado. No tienes los permisos necesarios.")));
    }

    @Test
    @DisplayName("Debería manejar una excepción genérica y devolver 500 Internal Server Error")
    @WithMockUser
    void handleGenericException_ShouldReturn500() throws Exception {
        mockMvc.perform(get("/test/generic-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", is("Ocurrió un error interno inesperado.")));
    }
}
