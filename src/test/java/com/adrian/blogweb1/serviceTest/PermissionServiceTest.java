package com.adrian.blogweb1.serviceTest;

import com.adrian.blogweb1.model.Permission;
import com.adrian.blogweb1.repository.IPermissionRepository;
import com.adrian.blogweb1.service.PermissionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private IPermissionRepository permissionRepository;

    @InjectMocks
    private PermissionService permissionService;

    @Test
    @DisplayName("findAll debería devolver una lista de todos los permisos")
    void findAll_ShouldReturnAllPermissions() {
        // --- 1. Arrange ---
        Permission perm1 = new Permission("READ");
        Permission perm2 = new Permission("WRITE");
        List<Permission> permissionList = List.of(perm1, perm2);

        // Guion: Cuando se llame a findAll, devuelve la lista de prueba.
        when(permissionRepository.findAll()).thenReturn(permissionList);

        // --- 2. Act ---
        List<Permission> resultado = permissionService.findAll();

        // --- 3. Assert ---
        assertThat(resultado).isNotNull();
        assertThat(resultado).hasSize(2);
        assertThat(resultado).containsExactly(perm1, perm2);
    }

    @Test
    @DisplayName("findById debería devolver un permiso cuando el ID existe")
    void findById_WhenIdExists_ShouldReturnPermission() {
        // --- 1. Arrange ---
        long permissionId = 1L;
        Permission permisoDePrueba = new Permission("READ");
        permisoDePrueba.setIdPermission(permissionId);

        // Guion: Cuando se busque el permiso con ID 1, se encontrará.
        when(permissionRepository.findById(permissionId)).thenReturn(Optional.of(permisoDePrueba));

        // --- 2. Act ---
        Optional<Permission> resultado = permissionService.findById(permissionId);

        // --- 3. Assert ---
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getPermissionName()).isEqualTo("READ");
    }

    @Test
    @DisplayName("findById debería devolver un Optional vacío cuando el ID no existe")
    void findById_WhenIdDoesNotExist_ShouldReturnEmpty() {
        // --- 1. Arrange ---
        long permissionIdQueNoExiste = 99L;

        // Guion: Cuando se busque el permiso con ID 99, no se encontrará nada.
        when(permissionRepository.findById(permissionIdQueNoExiste)).thenReturn(Optional.empty());

        // --- 2. Act ---
        Optional<Permission> resultado = permissionService.findById(permissionIdQueNoExiste);

        // --- 3. Assert ---
        assertThat(resultado).isNotPresent();
    }
}
