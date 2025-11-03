package com.adrian.blogweb1.security.config;

import com.adrian.blogweb1.service.DatabaseInitializationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// Usamos @SpringBootTest para cargar el contexto completo de la aplicación,
// lo que activará el CommandLineRunner.
// IMPORTANTE: NO usamos @ActiveProfiles("test") aquí, para que la clase
// DefaultUserConfig (que tiene @Profile("!test")) sea cargada.
@SpringBootTest
class DefaultUserConfigTest {

    // Reemplazamos el servicio real con un mock para no tocar la base de datos.
    @MockBean
    private DatabaseInitializationService initializationService;

    @Test
    @DisplayName("Debería llamar al servicio de inicialización al arrancar la aplicación")
    void initializeDatabase_shouldBeCalledOnStartup() {
        // Al levantar el contexto con @SpringBootTest, el CommandLineRunner ya se ha ejecutado.
        // Solo necesitamos verificar que nuestro servicio mock fue llamado.
        verify(initializationService, times(1)).initializeDatabase();
    }
}