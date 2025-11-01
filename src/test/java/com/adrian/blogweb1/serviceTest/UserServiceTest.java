package com.adrian.blogweb1.serviceTest;

import com.adrian.blogweb1.exception.ResourceNotFoundException;
import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.model.UserSec;
import com.adrian.blogweb1.repository.IRoleRepository;
import com.adrian.blogweb1.repository.IUserRepository;
import com.adrian.blogweb1.security.config.props.DefaultAdminProperties;
import com.adrian.blogweb1.service.IRoleService;
import com.adrian.blogweb1.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


// 1. Le decimos a JUnit que use la extensión de Mockito.
// Esto activa las anotaciones @Mock y @InjectMocks.
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    // 2. @Mock: Crea un "doble de acción" (un mock) del repositorio.
    // Este objeto es falso y nosotros controlamos su comportamiento.
    @Mock
    private IUserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;


    @Mock
    private IRoleService roleService;

    @Mock
    private IRoleRepository roleRepository; // Necesario para el test de createDefaultUser

    @Mock
    private DefaultAdminProperties adminProperties; // Necesario para el test de createDefaultUser


    // 3. @InjectMocks: Crea una instancia REAL de UserService, pero en lugar de inyectarle
    // un IUserRepository real, le inyecta el MOCK que creamos arriba.
    @InjectMocks
    private UserService userService;


    // --- INICIO DE LA SOLUCIÓN ---
    // Este método se ejecutará antes de cada test en esta clase.
    @BeforeEach
    void setUp() {
        // Después de que Mockito haya creado la instancia de 'userService' con @InjectMocks,
        // simulamos manualmente la auto-inyección que Spring haría en producción.
        // Esto asegura que el campo 'userService' interno no sea nulo.
        userService.setUserService(userService);
    }

    @Test
    @DisplayName("Debería devolver un usuario cuando el ID existe")
    void findById_WhenUserExists_ShouldReturnUser() {
        // --- 1. Arrange (Preparar el escenario) ---

        // a) Creamos los datos de prueba que usaremos en este test.
        long userId = 1L;
        UserSec userDePrueba = new UserSec();
        userDePrueba.setIdUserSec(userId);
        userDePrueba.setUsername("testuser");

        // b) Le damos el "guion" a nuestro actor (el mock del repositorio).
        // Le decimos: "CUANDO alguien (en este caso, el userService) llame a tu método findById
        // con el argumento 'userId' (que es 1L), ENTONCES quiero que devuelvas un Optional
        // que contenga nuestro userDePrueba".
        when(userRepository.findById(userId)).thenReturn(Optional.of(userDePrueba));

        // --- 2. Act (Actuar - Ejecutar el método que queremos probar) ---

        // Llamamos al método findById de nuestro userService REAL.
        // Internamente, este método llamará al findById de su repositorio,
        // pero como le hemos inyectado el MOCK, en realidad está llamando a nuestro actor.
        Optional<UserSec> resultado = userService.findById(userId);

        // --- 3. Assert (Verificar el resultado) ---

        // Usamos la librería AssertJ (que ya tienes en tu proyecto) para hacer las verificaciones.
        // Es mucho más legible que los asserts de JUnit por defecto.

        // ¿El Optional que nos devolvió el servicio contiene algo? Debería.
        assertThat(resultado).isPresent();
        // Si contiene algo, ¿el username del usuario que hay dentro es "testuser"?
        assertThat(resultado.get().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Debería devolver un Optional vacío cuando el ID no existe")
    void findById_WhenUserDoesNotExist_ShouldReturnEmpty() {
        // --- 1. Arrange (Preparar el escenario) ---

        // a) Definimos un ID que vamos a asumir que no existe.
        long userIdQueNoExiste = 99L;

        // b) Le damos un guion diferente a nuestro actor (el mock).
        // "CUANDO alguien llame a tu método findById con el ID 99L,
        // ENTONCES devuelve un Optional completamente vacío".
        when(userRepository.findById(userIdQueNoExiste)).thenReturn(Optional.empty());

        // --- 2. Act (Actuar) ---

        // Llamamos al método que queremos probar con el ID que no existe.
        Optional<UserSec> resultado = userService.findById(userIdQueNoExiste);

        // --- 3. Assert (Verificar) ---

        // Verificamos que el Optional que nos devolvió el servicio está, efectivamente, vacío.
        // isNotPresent() es lo contrario a isPresent() que usamos en el test anterior.
        assertThat(resultado).isNotPresent();
    }

    @Test
    @DisplayName("Debería llamar al método delete del repositorio cuando el usuario existe")

    void deleteUser_WhenUserExists_ShouldCallDelete() {
        // --- 1. Arrange (Preparar) ---
        long userId = 1L;
        UserSec userDePrueba = new UserSec();
        userDePrueba.setIdUserSec(userId);

        // Para borrar, el servicio primero busca al usuario.
        // Le damos el guion al mock: "Cuando te busquen por el ID 1L, devuelve el usuario de prueba".
        when(userRepository.findById(userId)).thenReturn(Optional.of(userDePrueba));

        // --- 2. Act (Actuar) ---
        userService.deleteUser(userId);

        // --- 3. Assert (Verificar) ---
        // Este es un tipo de verificación diferente. No nos importa qué devuelve el método (es void),
        // sino qué ACCIONES se realizaron.
        // Queremos verificar que el método 'delete' de nuestro repositorio FALSO fue llamado
        // exactamente 1 vez con el 'userDePrueba' como argumento.
        verify(userRepository, times(1)).delete(userDePrueba);
    }

    @Test
    @DisplayName("Debería lanzar ResourceNotFoundException al intentar borrar un usuario que no existe")
    void deleteUser_WhenUserDoesNotExist_ShouldThrowException() {
        // --- 1. Arrange ---
        long userIdQueNoExiste = 99L;

        // Guion: Cuando se busque por el ID 99L, no se encontrará nada.
        when(userRepository.findById(userIdQueNoExiste)).thenReturn(Optional.empty());

        // --- 2. Act & 3. Assert ---
        // Verificamos que se lanza la excepción correcta.
        assertThrows(ResourceNotFoundException.class, () -> {
            userService.deleteUser(userIdQueNoExiste);
        });

        verify(userRepository, never()).delete(any(UserSec.class));
    }

    @Test
    @DisplayName("Debería encriptar la contraseña antes de guardar el usuario")
    void save_ShouldEncryptPasswordBeforeSaving() {
        // --- 1. Arrange (Preparar) ---

        // a) Creamos el usuario que vamos a "enviar" al servicio.
        // Tiene la contraseña en texto plano.
        UserSec usuarioSinGuardar = new UserSec();
        usuarioSinGuardar.setUsername("newUser");
        usuarioSinGuardar.setPassword("password123");

        String passwordEncriptada = "super_secreto_encriptado_#123";

        // b) Le damos el guion a nuestros actores (los mocks).
        // Guion para el PasswordEncoder: "CUANDO te llamen con 'password123', devuelve esta cadena encriptada".
        when(passwordEncoder.encode("password123")).thenReturn(passwordEncriptada);

        // Guion para el UserRepository: "CUANDO te llamen para guardar CUALQUIER objeto UserSec,
        // simplemente devuelve el mismo objeto que te pasaron".
        // Usamos 'any()' porque el objeto que recibe el repositorio ya tendrá la contraseña encriptada,
        // y no queremos crear una instancia exacta de eso aquí.
        when(userRepository.save(any(UserSec.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // --- 2. Act (Actuar) ---

        // Llamamos al método save que queremos probar.
        UserSec usuarioGuardado = userService.save(usuarioSinGuardar);

        // --- 3. Assert (Verificar) ---

        // Verificamos que el usuario que nos devuelve el método tiene la contraseña encriptada.
        assertThat(usuarioGuardado.getPassword()).isEqualTo(passwordEncriptada);

        // Verificación extra (opcional pero muy buena práctica):
        // Verificamos que el método 'encode' del passwordEncoder fue llamado exactamente 1 vez.
        verify(passwordEncoder, times(1)).encode("password123");
    }

    @Test
    @DisplayName("Debería lanzar ResourceNotFoundException al intentar actualizar un usuario que no existe")
    void updateUser_WhenUserDoesNotExist_ShouldThrowException() {
        // --- 1. Arrange (Preparar) ---
        long userIdQueNoExiste = 99L;
        UserSec detallesNuevos = new UserSec(); // Los detalles no importan para este test

        // Le damos el guion al mock: "Cuando busquen el ID 99L, devuelve un Optional vacío".

        when(userRepository.findById(userIdQueNoExiste)).thenReturn(Optional.empty());

        // --- 2. Act & 3. Assert (Actuar y Verificar) ---
        // Verificamos que se lanza la excepción correcta cuando llamamos al método.
        assertThrows(ResourceNotFoundException.class,
                () -> {
                    userService.updateUser(userIdQueNoExiste, detallesNuevos);
                });

        // Verificación extra: Nos aseguramos de que el método 'save' del repositorio NUNCA fue llamado.
        verify(userRepository, never()).save(any(UserSec.class));
    }

    @Test
    @DisplayName("Debería actualizar los campos del usuario y guardarlo cuando el usuario existe")
    void updateUser_WhenUserExists_ShouldUpdateAndSaveUser() {
        // --- 1. Arrange (Preparar) ---

        // a) Datos del usuario que YA EXISTE en la "base de datos"
        long userId = 1L;
        UserSec usuarioExistente = new UserSec();
        usuarioExistente.setIdUserSec(userId);
        usuarioExistente.setUsername("usuario_antiguo");
        usuarioExistente.setEmail("email_antiguo@test.com");

        // b) Datos con los que queremos ACTUALIZAR al usuario
        UserSec detallesNuevos = new UserSec();
        detallesNuevos.setUsername("usuario_nuevo");
        detallesNuevos.setEmail("email_nuevo@test.com");

        // c) Le damos el guion al mock del repositorio
        // "CUANDO busquen por el ID 1L, devuelve el usuario existente".
        when(userRepository.findById(userId)).thenReturn(Optional.of(usuarioExistente));

        // "CUANDO guarden CUALQUIER usuario, simplemente devuelve el mismo que te pasaron".
        when(userRepository.save(any(UserSec.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // d) ¡LA HERRAMIENTA NUEVA! Creamos un "capturador de argumentos".
        // Es como poner un micrófono oculto en el método 'save' del repositorio
        // para grabar exactamente qué objeto se le pasó.
        ArgumentCaptor<UserSec> userArgumentCaptor = ArgumentCaptor.forClass(UserSec.class);

        // --- 2. Act (Actuar) ---

        // Llamamos al método que queremos probar.
        userService.updateUser(userId, detallesNuevos);

        // --- 3. Assert (Verificar) ---

        // a) Usamos nuestro "micrófono" para capturar el argumento que se le pasó al método save.
        // Le decimos: "Verifica que el método 'save' fue llamado 1 vez, y mientras lo haces,
        // captura el objeto UserSec que se le pasó".
        verify(userRepository, times(1)).save(userArgumentCaptor.capture());

        // b) Obtenemos el valor que fue capturado.
        UserSec usuarioCapturado = userArgumentCaptor.getValue();

        // c) Ahora, hacemos las verificaciones sobre el objeto que REALMENTE se guardó.
        // ¿El username del usuario guardado es el nuevo?
        assertThat(usuarioCapturado.getUsername()).isEqualTo("usuario_nuevo");
        // ¿El email del usuario guardado es el nuevo?
        assertThat(usuarioCapturado.getEmail()).isEqualTo("email_nuevo@test.com");
        // ¿El ID sigue siendo el mismo? (¡Importante!)
        assertThat(usuarioCapturado.getIdUserSec()).isEqualTo(userId);
    }

    @Test
    @DisplayName("updateUser debería encriptar la contraseña si se proporciona una nueva")
    void updateUser_WhenNewPasswordIsProvided_ShouldEncryptPassword() {
        // --- 1. Arrange ---
        long userId = 1L;
        UserSec usuarioExistente = new UserSec();
        usuarioExistente.setIdUserSec(userId);
        usuarioExistente.setPassword("hash_antiguo");

        UserSec detallesNuevos = new UserSec();
        detallesNuevos.setPassword("nueva_password_123");

        String passwordEncriptada = "nueva_password_encriptada";

        when(userRepository.findById(userId)).thenReturn(Optional.of(usuarioExistente));
        when(passwordEncoder.encode("nueva_password_123")).thenReturn(passwordEncriptada);
        when(userRepository.save(any(UserSec.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // --- 2. Act ---
        UserSec usuarioActualizado = userService.updateUser(userId, detallesNuevos);

        // --- 3. Assert ---
        // Verificamos que el método 'encode' fue llamado con la nueva contraseña.
        verify(passwordEncoder, times(1)).encode("nueva_password_123");
        // Verificamos que la contraseña del usuario actualizado es la nueva y encriptada.
        assertThat(usuarioActualizado.getPassword()).isEqualTo(passwordEncriptada);
    }

    @Test
    @DisplayName("updateUser debería actualizar la lista de roles del usuario")
    void updateUser_ShouldUpdateUserRoles() {
        // --- 1. Arrange ---
        long userId = 1L;
        UserSec usuarioExistente = new UserSec();
        usuarioExistente.setIdUserSec(userId);

        Role rolNuevo = new Role();
        rolNuevo.setIdRole(10L);
        rolNuevo.setRole("ROLE_SUPERVISOR");

        UserSec detallesNuevos = new UserSec();
        detallesNuevos.setRolesList(Set.of(rolNuevo));

        when(userRepository.findById(userId)).thenReturn(Optional.of(usuarioExistente));
        // Guion: Cuando el servicio busque los roles por sus IDs, los encontrará.
        when(roleService.findAllByIds(Set.of(10L))).thenReturn(Set.of(rolNuevo));
        when(userRepository.save(any(UserSec.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArgumentCaptor<UserSec> userCaptor = ArgumentCaptor.forClass(UserSec.class);

        // --- 2. Act ---
        userService.updateUser(userId, detallesNuevos);

        // --- 3. Assert ---
        // Capturamos el usuario que se va a guardar.
        verify(userRepository).save(userCaptor.capture());
        UserSec usuarioGuardado = userCaptor.getValue();

        // Verificamos que la lista de roles del usuario guardado contiene el nuevo rol.
        assertThat(usuarioGuardado.getRolesList()).isNotNull();
        assertThat(usuarioGuardado.getRolesList()).hasSize(1);
        assertThat(usuarioGuardado.getRolesList()).contains(rolNuevo);
    }

    @Test
    @DisplayName("findOrCreateUserForOAuth debería devolver el usuario existente si lo encuentra por email")
    void findOrCreateUserForOAuth_WhenUserExists_ShouldReturnExistingUser() {
        // --- 1. Arrange ---
        String email = "existente@example.com";
        String username = "usuario_existente";

        UserSec existingUser = new UserSec();
        existingUser.setIdUserSec(1L);
        existingUser.setEmail(email);

        // Guion: Cuando se busque por este email, se encontrará al usuario existente.
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));

        // --- 2. Act ---
        UserSec resultUser = userService.findOrCreateUserForOAuth(email, username);

        // --- 3. Assert ---
        // Verificamos que el usuario devuelto es el que ya existía.
        assertThat(resultUser.getIdUserSec()).isEqualTo(1L);
        assertThat(resultUser.getEmail()).isEqualTo(email);

        // Verificación crucial: Nos aseguramos de que el método 'save' NUNCA fue llamado.
        verify(userRepository, never()).save(any(UserSec.class));
    }

    @Test
    @DisplayName("findOrCreateUserForOAuth debería crear un nuevo usuario si no lo encuentra por email")
    void findOrCreateUserForOAuth_WhenUserDoesNotExist_ShouldCreateAndReturnNewUser() {
        // --- 1. Arrange ---
        String email = "nuevo_oauth@example.com";
        String username = "nuevo_oauth";

        Role userRole = new Role();
        userRole.setRole("ROLE_USER");

        // Guion 1: Cuando se busque por email, no se encontrará nada.
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        // Guion 2: Cuando se busque el rol por defecto, se encontrará.
        when(roleService.findByRoleName("ROLE_USER")).thenReturn(Optional.of(userRole));
        // Guion 3: Cuando se guarde el nuevo usuario, se devolverá ese mismo usuario.
        when(userRepository.save(any(UserSec.class))).thenAnswer(invocation -> {
            UserSec userToSave = invocation.getArgument(0);
            userToSave.setIdUserSec(2L); // Simulamos que la BD le asigna un ID
            return userToSave;
        });

        // --- 2. Act ---
        UserSec resultUser = userService.findOrCreateUserForOAuth(email, username);

        // --- 3. Assert ---
        assertThat(resultUser).isNotNull();
        assertThat(resultUser.getEmail()).isEqualTo(email);
        assertThat(resultUser.getUsername()).isEqualTo(username);
        assertThat(resultUser.getRolesList()).contains(userRole);

        // Verificamos que el método 'save' fue llamado exactamente 1 vez.
        verify(userRepository, times(1)).save(any(UserSec.class));
    }

    @Test
    @DisplayName("createDefaultUser no debería hacer nada si el usuario admin ya existe")
    void createDefaultUser_WhenAdminAlreadyExists_ShouldDoNothing() {
        // --- 1. Arrange ---
        String adminEmail = "admin@test.com";
        when(adminProperties.getEmail()).thenReturn(adminEmail);
        when(userRepository.findByEmail(adminEmail)).thenReturn(Optional.of(new UserSec()));

        // --- 2. Act ---
        userService.createDefaultUser();

        // --- 3. Assert ---
        // Verificamos que el método 'save' NUNCA fue llamado.
        verify(userRepository, never()).save(any(UserSec.class));
    }

    @Test
    @DisplayName("findAll debería devolver una lista de todos los usuarios")
    void findAll_ShouldReturnAllUsers() {
        // --- 1. Arrange ---
        UserSec user1 = new UserSec();
        user1.setUsername("user1");
        UserSec user2 = new UserSec();
        user2.setUsername("user2");
        List<UserSec> userList = List.of(user1, user2);

        // Guion: Cuando se llame a findAll, devuelve la lista de prueba.
        when(userRepository.findAll()).thenReturn(userList);

        // --- 2. Act ---
        List<UserSec> resultado = userService.findAll();

        // --- 3. Assert ---
        assertThat(resultado).isNotNull();
        assertThat(resultado).hasSize(2);
        assertThat(resultado).containsExactly(user1, user2);
    }


    @Test
    @DisplayName("createDefaultUser debería crear el usuario admin si no existe")
    void createDefaultUser_WhenAdminDoesNotExist_ShouldCreateAdmin() {
        // --- 1. Arrange ---
        String adminEmail = "admin@test.com";

        // Guion 1: Cuando se pidan las propiedades del admin, se devolverá el email.
        when(adminProperties.getEmail()).thenReturn(adminEmail);

        // Guion 2: Cuando se busque por el email del admin, no se encontrará nada.
        when(userRepository.findByEmail(adminEmail)).thenReturn(Optional.empty());

        // Guion 3: Cuando se busque el rol ADMIN, se encontrará.
        Role adminRole = new Role();
        adminRole.setRole("ROLE_ADMIN");
        when(roleRepository.findByRole("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));

        // --- 2. Act ---
        userService.createDefaultUser();

        // --- 3. Assert ---
        // Verificamos que el método 'save' fue llamado exactamente 1 vez.
        verify(userRepository, times(1)).save(any(UserSec.class));
    }



}
