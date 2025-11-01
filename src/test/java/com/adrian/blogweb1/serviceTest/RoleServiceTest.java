package com.adrian.blogweb1.serviceTest;


import com.adrian.blogweb1.exception.ResourceNotFoundException;
import com.adrian.blogweb1.model.Permission;
import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.model.UserSec;
import com.adrian.blogweb1.repository.IPermissionRepository;
import com.adrian.blogweb1.repository.IRoleRepository;
import com.adrian.blogweb1.repository.IUserRepository;
import com.adrian.blogweb1.service.DatabaseInitializationService;
import com.adrian.blogweb1.service.RoleService;
import com.adrian.blogweb1.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    private IRoleRepository roleRepository;

    @Mock
    private IPermissionRepository permissionRepository;

    @Mock
    private IUserRepository userRepository;


    @InjectMocks
    private RoleService roleService;

    @Test
    @DisplayName("findAll debería devolver una lista de todos los roles")
    void findAll_ShouldReturnAllRoles() {
        // --- 1. Arrange ---
        Role role1 = new Role();
        role1.setRole("ROLE_USER");
        Role role2 = new Role();
        role2.setRole("ROLE_ADMIN");
        List<Role> roleList = List.of(role1, role2);

        when(roleRepository.findAll()).thenReturn(roleList);

        // --- 2. Act ---
        List<Role> resultado = roleService.findAll();

        // --- 3. Assert ---
        assertThat(resultado).isNotNull();
        assertThat(resultado).hasSize(2);
        assertThat(resultado).containsExactly(role1, role2);
    }

    @Test
    @DisplayName("findById debería devolver un rol cuando el ID existe")
    void findById_WhenIdExists_ShouldReturnRole() {
        // --- 1. Arrange ---
        long roleId = 1L;
        Role rolDePrueba = new Role();
        rolDePrueba.setIdRole(roleId);
        rolDePrueba.setRole("ROLE_TEST");

        when(roleRepository.findById(roleId)).thenReturn(Optional.of(rolDePrueba));

        // --- 2. Act ---
        Optional<Role> resultado = roleService.findById(roleId);

        // --- 3. Assert ---
        assertThat(resultado).isPresent();
        assertThat(resultado.get().getRole()).isEqualTo("ROLE_TEST");
    }

    @Test
    @DisplayName("deleteRole debería desasociar el rol de los usuarios y luego eliminarlo")
    void deleteRole_WhenRoleExists_ShouldDisassociateAndThenDelete() {
        // --- 1. Arrange ---
        long roleId = 1L;
        Role roleToDelete = new Role();
        roleToDelete.setIdRole(roleId);

        UserSec userWithRole = new UserSec();
        userWithRole.setRolesList(new HashSet<>(Set.of(roleToDelete)));

        // Guion 1: Cuando se busque el rol, se encontrará.
        when(roleRepository.findById(roleId)).thenReturn(Optional.of(roleToDelete));
        // Guion 2: Cuando se busquen usuarios con ese rol, se encontrará una lista con un usuario.
        when(userRepository.findByRolesListContains(roleToDelete)).thenReturn(List.of(userWithRole));

        // --- 2. Act ---
        roleService.deleteRole(roleId);

        // --- 3. Assert ---
        verify(userRepository, times(1)).saveAll(List.of(userWithRole)); // Verificamos que se guardan los usuarios actualizados
        verify(roleRepository, times(1)).delete(roleToDelete); // Verificamos que el rol se elimina
        assertThat(userWithRole.getRolesList()).isEmpty(); // Verificamos que la lista de roles del usuario ahora está vacía
    }


    @Test
    @DisplayName("deleteRole debería lanzar ResourceNotFoundException si el rol no existe")
    void deleteRole_WhenRoleDoesNotExist_ShouldThrowException() {
        // --- 1. Arrange ---
        long roleIdQueNoExiste = 99L;
        // CORRECCIÓN: Simulamos el comportamiento de findById, que es lo que el servicio usa.
        // "Cuando busquen el rol con ID 99, devuelve un Optional vacío".
        when(roleRepository.findById(roleIdQueNoExiste)).thenReturn(Optional.empty());

        // --- 2. Act & 3. Assert ---
        assertThrows(ResourceNotFoundException.class, () -> {
            roleService.deleteRole(roleIdQueNoExiste);
        });

        // CORRECCIÓN: Verificamos que el método delete(role) nunca fue llamado.
        verify(roleRepository, never()).delete(any(Role.class));
    }

    @Test
    @DisplayName("save debería guardar un nuevo rol y devolverlo")
    void save_ShouldSaveAndReturnRole() {
        // --- 1. Arrange ---
        // a) Creamos el rol que vamos a "enviar" al servicio.
        Role rolAEnviar = new Role();
        rolAEnviar.setRole("ROLE_GUEST");

        // b) Creamos el rol que SIMULAREMOS que la base de datos ha guardado.
        Role rolGuardado = new Role();
        rolGuardado.setIdRole(3L); // Un nuevo ID
        rolGuardado.setRole("ROLE_GUEST");

        // c) Guion para el mock del repositorio: "CUANDO te llamen para guardar CUALQUIER rol,
        // devuelve el objeto 'rolGuardado' que hemos creado".
        when(roleRepository.save(any(Role.class))).thenReturn(rolGuardado);

        // --- 2. Act ---
        Role resultado = roleService.save(rolAEnviar);

        // --- 3. Assert ---
        assertThat(resultado).isNotNull();
        assertThat(resultado.getIdRole()).isEqualTo(3L);
        assertThat(resultado.getRole()).isEqualTo("ROLE_GUEST");
        verify(roleRepository, times(1)).save(rolAEnviar);
    }

    @ExtendWith(MockitoExtension.class)
    static
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
            // Encadenamos las respuestas de Mockito.
            // Las primeras 4 llamadas a findByPermissionName devolverán empty() para activar la creación.
            // Las siguientes llamadas devolverán un permiso para que la creación de roles funcione.
            when(permissionRepository.findByPermissionName(anyString()))
                    .thenReturn(Optional.empty()) // para createPermissionIfNotFound("READ")
                    .thenReturn(Optional.empty()) // para createPermissionIfNotFound("CREATE")
                    .thenReturn(Optional.empty()) // para createPermissionIfNotFound("UPDATE")
                    .thenReturn(Optional.empty()) // para createPermissionIfNotFound("DELETE")
                    .thenAnswer(invocation -> Optional.of(new Permission(invocation.getArgument(0)))); // para createRoleIfNotFound

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
            when(permissionRepository.findByPermissionName(anyString())).thenReturn(Optional.of(new Permission()));
            // --- INICIO DE LA SOLUCIÓN ---
            // Añadimos la regla que faltaba: le decimos a Mockito qué hacer cuando se busque "ROLE_USER".
            when(roleRepository.findByRole("ROLE_USER")).thenReturn(Optional.of(new Role()));
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
}
