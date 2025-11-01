package com.adrian.blogweb1.serviceTest;

import com.adrian.blogweb1.model.Permission;
import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.repository.IPermissionRepository;
import com.adrian.blogweb1.repository.IRoleRepository;
import com.adrian.blogweb1.service.DatabaseInitializationService;
import com.adrian.blogweb1.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseInitializationServiceTest {

    @Mock
    private IRoleRepository roleRepository;

    @Mock
    private IPermissionRepository permissionRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private DatabaseInitializationService initializationService;

    @Test
    @DisplayName("Debería crear permisos y roles si no existen")
    void initializeDatabase_whenDataDoesNotExist_shouldCreateData() {
        // --- 1. Arrange ---
        // --- INICIO DE LA SOLUCIÓN ---
        // Simulamos que los repositorios no encuentran nada al principio.
        // Usamos thenAnswer para que, después de la primera llamada (que devuelve empty),
        // las siguientes llamadas (dentro de createRoleIfNotFound) sí encuentren el permiso.
        when(permissionRepository.findByPermissionName("READ")).thenReturn(Optional.empty()).thenAnswer(inv -> Optional.of(new Permission("READ")));
        when(permissionRepository.findByPermissionName("CREATE")).thenReturn(Optional.empty()).thenAnswer(inv -> Optional.of(new Permission("CREATE")));
        when(permissionRepository.findByPermissionName("UPDATE")).thenReturn(Optional.empty()).thenAnswer(inv -> Optional.of(new Permission("UPDATE")));
        when(permissionRepository.findByPermissionName("DELETE")).thenReturn(Optional.empty()).thenAnswer(inv -> Optional.of(new Permission("DELETE")));

        when(roleRepository.findByRole(anyString())).thenReturn(Optional.empty());
        // --- FIN DE LA SOLUCIÓN ---

        // --- 2. Act ---
        initializationService.initializeDatabase();

        // --- 3. Assert ---
        // Verificamos que se intentó guardar cada permiso
        verify(permissionRepository, times(4)).save(any(Permission.class));
        // Verificamos que se intentó guardar cada rol
        verify(roleRepository, times(2)).save(any(Role.class));
        // Verificamos que se llamó al método para crear el usuario por defecto
        verify(userService, times(1)).createDefaultUser();
    }

    @Test
    @DisplayName("No debería crear nada si los permisos y roles ya existen")
    void initializeDatabase_whenDataExists_shouldNotCreateData() {
        // --- 1. Arrange ---
        // Simulamos que todos los permisos y roles ya existen
        when(permissionRepository.findByPermissionName(anyString())).thenReturn(Optional.of(new Permission()));
        when(roleRepository.findByRole(anyString())).thenReturn(Optional.of(new Role()));

        // --- 2. Act ---
        initializationService.initializeDatabase();

        // --- 3. Assert ---
        // Verificamos que NUNCA se llamó al método save, porque todo ya existía
        verify(permissionRepository, never()).save(any(Permission.class));
        verify(roleRepository, never()).save(any(Role.class));
        // Verificamos que, a pesar de todo, sí se llama al método para crear el usuario por defecto
        verify(userService, times(1)).createDefaultUser();
    }

    @Test
    @DisplayName("Debería lanzar una excepción si un permiso requerido para un rol no existe")
    void initializeDatabase_whenPermissionForRoleIsMissing_shouldThrowException() {
        // --- 1. Arrange ---
        // Simulamos que los permisos existen, pero el rol "ROLE_ADMIN" no.
        when(permissionRepository.findByPermissionName(anyString())).thenReturn(Optional.of(new Permission("ANY")));
        // --- INICIO DE LA SOLUCIÓN ---
        // Añadimos la regla que faltaba: le decimos a Mockito qué hacer cuando se busque "ROLE_USER".
        when(roleRepository.findByRole("ROLE_USER")).thenReturn(Optional.of(new Role()));
        // --- FIN DE LA SOLUCIÓN ---
        when(roleRepository.findByRole("ROLE_ADMIN")).thenReturn(Optional.empty());

        // Crucial: Simulamos que uno de los permisos necesarios para ROLE_ADMIN no se encuentra
        when(permissionRepository.findByPermissionName("UPDATE")).thenReturn(Optional.empty());

        // --- 2. Act & 3. Assert ---
        // Verificamos que se lanza la excepción correcta cuando se intenta crear el rol
        // y no se encuentra uno de sus permisos.
        assertThrows(IllegalStateException.class, () -> {
            initializationService.initializeDatabase();
        });

        // Verificamos que no se llegó a guardar ningún rol
        verify(roleRepository, never()).save(any(Role.class));
    }
}
