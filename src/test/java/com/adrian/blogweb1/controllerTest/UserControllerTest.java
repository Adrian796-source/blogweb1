package com.adrian.blogweb1.controllerTest;

import com.adrian.blogweb1.controller.UserController;
import com.adrian.blogweb1.exception.ResourceNotFoundException;
import com.adrian.blogweb1.model.Role;
import com.adrian.blogweb1.model.UserSec;
import com.adrian.blogweb1.service.IRoleService;
import com.adrian.blogweb1.service.IUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.List;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    // Mockeamos las dependencias del UserController
    @Mock
    private IUserService userService;

    @Mock
    private IRoleService roleService;

    // Creamos una instancia real del controlador
    @InjectMocks
    private UserController userController;

    // --- INICIO DE LA SOLUCIÓN: NUEVOS TESTS PARA COBERTURA ---

    @Test
    @DisplayName("Debería devolver 200 OK y una lista de todos los usuarios")
    void getAllUsers_ShouldReturnListOfUsers() {
        // --- 1. Arrange ---
        when(userService.findAll()).thenReturn(List.of(new UserSec(), new UserSec()));

        // --- 2. Act ---
        ResponseEntity<List<UserSec>> respuesta = userController.getAllUsers();

        // --- 3. Assert ---
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).hasSize(2);
    }

    @Test
    @DisplayName("Debería devolver 200 OK y un usuario cuando el ID existe")
    void getUserById_WhenIdExists_ShouldReturnOkAndUser() {
        // --- 1. Arrange ---
        long userId = 1L;
        UserSec userDePrueba = new UserSec();
        userDePrueba.setIdUserSec(userId);
        userDePrueba.setUsername("testuser");

        // Guion para el mock del servicio: "Cuando te pidan el usuario con ID 1, devuelve esto".
        when(userService.findById(userId)).thenReturn(Optional.of(userDePrueba));

        // --- 2. Act ---
        ResponseEntity<UserSec> respuesta = userController.getUserById(userId);

        // --- 3. Assert ---
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Debería devolver 404 Not Found cuando el ID del usuario no existe")
    void getUserById_WhenIdDoesNotExist_ShouldReturnNotFound() {
        // --- 1. Arrange ---
        long userIdQueNoExiste = 99L;

        // Guion para el mock del servicio: "Cuando te pidan el usuario con ID 99,
        // devuelve un Optional vacío porque no existe".
        when(userService.findById(userIdQueNoExiste)).thenReturn(Optional.empty());

        // --- 2. Act ---
        ResponseEntity<UserSec> respuesta = userController.getUserById(userIdQueNoExiste);

        // --- 3. Assert ---
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).isNull();
    }

    @Test
    @DisplayName("Debería devolver 201 Created y el nuevo usuario cuando los datos son válidos")
    void createUser_WhenDataIsValid_ShouldReturnCreatedAndNewUser() throws Exception {
        // --- 1. Arrange ---

        // a) Creamos el rol que simularemos que existe en la BD
        Role rolDePrueba = new Role();
        rolDePrueba.setIdRole(1L);
        rolDePrueba.setRole("ROLE_USER");

        // b) Creamos el usuario que "enviaremos" en la petición
        UserSec usuarioAEnviar = new UserSec();
        usuarioAEnviar.setPassword("password123");
        // La petición vendría con una lista de roles (solo con el ID)
        usuarioAEnviar.setRolesList(Set.of(rolDePrueba));

        // c) Creamos el usuario que simularemos que se guarda en la BD
        UserSec usuarioGuardado = new UserSec();
        usuarioGuardado.setIdUserSec(100L);
        usuarioGuardado.setUsername("nuevoUsuario");

        // d) Guiones para los mocks
        // Cuando el userService necesite encriptar "password123", devolverá "encriptada"
        when(userService.encryptPassword("password123")).thenReturn("encriptada");
        // Cuando el roleService busque el rol con ID 1L, lo encontrará
        when(roleService.findById(1L)).thenReturn(Optional.of(rolDePrueba));
        // Cuando el userService guarde CUALQUIER usuario, devolverá nuestro 'usuarioGuardado'
        when(userService.save(any(UserSec.class))).thenReturn(usuarioGuardado);

        // --- 2. Act ---
        ResponseEntity<Object> respuesta = userController.createUser(usuarioAEnviar);

        // --- 3. Assert ---
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isNotNull();

        // Hacemos un cast para verificar el cuerpo de la respuesta
        UserSec usuarioEnRespuesta = (UserSec) respuesta.getBody();
        assertThat(usuarioEnRespuesta.getIdUserSec()).isEqualTo(100L);
    }

    @Test
    @DisplayName("Debería devolver 400 Bad Request si se intenta crear un usuario sin contraseña")
    void createUser_WhenPasswordIsNull_ShouldReturnBadRequest() {
        // --- 1. Arrange ---

        // a) Creamos un usuario sin contraseña
        UserSec usuarioSinPassword = new UserSec();
        usuarioSinPassword.setRolesList(Set.of(new Role())); // Añadimos un rol para pasar la segunda validación

        // --- 2. Act ---
        ResponseEntity<Object> respuesta = userController.createUser(usuarioSinPassword);

        // --- 3. Assert ---
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).isNotNull();

        // Verificamos que el cuerpo del mensaje es el correcto
        Map<String, String> responseBody = (Map<String, String>) respuesta.getBody();
        assertThat(responseBody.get("message")).isEqualTo("El campo 'password' no puede estar vacío");

        // Verificación extra: Nos aseguramos de que NUNCA se llamó al servicio para guardar
        verify(userService, never()).save(any(UserSec.class));
    }

    @Test
    @DisplayName("Debería devolver 400 Bad Request si se intenta crear un usuario sin roles")
    void createUser_WhenRolesListIsNull_ShouldReturnBadRequest() {
        // --- 1. Arrange ---

        // a) Creamos un usuario con contraseña pero sin lista de roles
        UserSec usuarioSinRoles = new UserSec();
        usuarioSinRoles.setPassword("password123");
        usuarioSinRoles.setRolesList(null); // Explícitamente nulo

        // --- 2. Act ---
        ResponseEntity<Object> respuesta = userController.createUser(usuarioSinRoles);

        // --- 3. Assert ---
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody()).isNotNull();

        // Verificamos que el cuerpo del mensaje es el correcto
        Map<String, String> responseBody = (Map<String, String>) respuesta.getBody();
        assertThat(responseBody.get("message")).isEqualTo("El campo 'rolesList' no puede estar vacío");

        // Verificación extra: Nos aseguramos de que NUNCA se llamó al servicio para guardar
        verify(userService, never()).save(any(UserSec.class));
    }

    @Test
    @DisplayName("Debería devolver 200 OK cuando se elimina un usuario existente")
    void deleteUser_WhenIdExists_ShouldReturnOk() {
        // --- 1. Arrange ---
        long userId = 1L;

        // No necesitamos un 'when' para el servicio, ya que el método 'deleteUser' es void.
        // Mockito simplemente lo ejecutará sin lanzar error.

        // --- 2. Act ---
        ResponseEntity<Map<String, String>> respuesta = userController.deleteUser(userId);

        // --- 3. Assert ---
        // Verificamos que el controlador llamó al método correcto del servicio.
        verify(userService, times(1)).deleteUser(userId);

        // Verificamos que la respuesta HTTP es la correcta.
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().get("message")).isEqualTo("Usuario eliminado correctamente");
    }

    @Test
    @DisplayName("Debería devolver 404 Not Found cuando se intenta eliminar un usuario que no existe")
    void deleteUser_WhenIdDoesNotExist_ShouldReturnNotFound() {
        // --- 1. Arrange ---
        long userIdQueNoExiste = 99L;

        // Le damos el guion al mock del servicio.
        // "CUANDO alguien llame a tu método deleteUser con el ID 99L,
        // LANZA una ResourceNotFoundException".
        doThrow(new ResourceNotFoundException("Usuario no encontrado")).when(userService).deleteUser(userIdQueNoExiste);

        // --- 2. Act ---
        ResponseEntity<Map<String, String>> respuesta = userController.deleteUser(userIdQueNoExiste);

        // --- 3. Assert ---
        // Verificamos que la respuesta HTTP es la de "no encontrado".
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).isNotNull();
        assertThat(respuesta.getBody().get("message")).isEqualTo("Usuario no encontrado");
    }

    @Test
    @DisplayName("Debería devolver 200 OK y el usuario actualizado cuando el ID existe")
    void updateUser_WhenUserExists_ShouldReturnOkAndUpdatedUser() {
        // --- 1. Arrange ---
        long userId = 1L;

        // a) Los detalles con los que queremos actualizar
        UserSec detallesNuevos = new UserSec();
        detallesNuevos.setUsername("usuario_nuevo");

        // b) El objeto que SIMULAREMOS que el servicio devuelve tras actualizar
        UserSec usuarioActualizado = new UserSec();
        usuarioActualizado.setIdUserSec(userId);
        usuarioActualizado.setUsername("usuario_nuevo");

        // c) Guion para el mock del servicio: "CUANDO te llamen para actualizar el usuario con ID 1L,
        // devuelve el objeto 'usuarioActualizado'".
        when(userService.updateUser(userId, detallesNuevos)).thenReturn(usuarioActualizado);

        // --- 2. Act ---
        ResponseEntity<Object> respuesta = userController.updateUser(userId, detallesNuevos);

        // --- 3. Assert ---
        // Verificamos que el método del servicio fue llamado con los argumentos correctos.
        verify(userService, times(1)).updateUser(userId, detallesNuevos);

        // Verificamos la respuesta HTTP.
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isNotNull();

        // Verificamos que el cuerpo de la respuesta es el usuario actualizado.
        UserSec usuarioEnRespuesta = (UserSec) respuesta.getBody();
        assertThat(usuarioEnRespuesta.getUsername()).isEqualTo("usuario_nuevo");
    }

    @Test
    @DisplayName("Debería devolver 404 Not Found al intentar actualizar un usuario que no existe")
    void updateUser_WhenUserDoesNotExist_ShouldReturnNotFound() {
        // --- 1. Arrange ---
        long userIdQueNoExiste = 99L;
        UserSec detallesNuevos = new UserSec();
        detallesNuevos.setUsername("un_nombre_cualquiera");

        // Guion para el mock del servicio: "CUANDO te llamen para actualizar el usuario con ID 99L,
        // LANZA una ResourceNotFoundException".
        when(userService.updateUser(userIdQueNoExiste, detallesNuevos))
                .thenThrow(new ResourceNotFoundException("Usuario no encontrado"));

        // --- 2. Act ---
        ResponseEntity<Object> respuesta = userController.updateUser(userIdQueNoExiste, detallesNuevos);

        // --- 3. Assert ---
        // Verificamos que la respuesta HTTP es la de "no encontrado".
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody()).isNotNull();

        // --- ¡LA CORRECCIÓN! ---
        // Hacemos un cast del cuerpo de la respuesta a un Map para verificar su contenido.
        Map<String, String> responseBody = (Map<String, String>) respuesta.getBody();
        assertThat(responseBody.get("message")).isEqualTo("Usuario no encontrado");
        assertThat(responseBody.get("status")).isEqualTo("error");
    }

    // --- INICIO DE LA SOLUCIÓN: NUEVOS TESTS PARA COBERTURA ---

    @Test
    @DisplayName("Debería devolver 400 Bad Request al crear un usuario si el rol no existe")
    void createUser_WhenRoleNotFound_ShouldReturnBadRequest() {
        // --- 1. Arrange ---
        Role rolInexistente = new Role();
        rolInexistente.setIdRole(99L);

        UserSec usuarioAEnviar = new UserSec();
        usuarioAEnviar.setPassword("password123");
        usuarioAEnviar.setRolesList(Set.of(rolInexistente));

        // Guion: Cuando se busque el rol con ID 99, no se encontrará.
        when(roleService.findById(99L)).thenReturn(Optional.empty());
        when(userService.encryptPassword(anyString())).thenReturn("encrypted");

        // --- 2. Act ---
        ResponseEntity<Object> respuesta = userController.createUser(usuarioAEnviar);

        // --- 3. Assert ---
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<String, String> responseBody = (Map<String, String>) respuesta.getBody();
        assertThat(responseBody.get("message")).isEqualTo("Rol no encontrado con ID: 99");
    }

    @Test
    @DisplayName("Debería devolver 500 Internal Server Error al crear un usuario si ocurre un error inesperado")
    void createUser_WhenUnexpectedError_ShouldReturnInternalServerError() {
        // --- 1. Arrange ---
        Role rolDePrueba = new Role();
        rolDePrueba.setIdRole(1L);

        UserSec usuarioAEnviar = new UserSec();
        usuarioAEnviar.setPassword("password123");
        usuarioAEnviar.setRolesList(Set.of(rolDePrueba));

        // Guion: Simulamos un error genérico en la capa de servicio.
        when(roleService.findById(1L)).thenThrow(new RuntimeException("Error de base de datos simulado"));

        // --- 2. Act ---
        ResponseEntity<Object> respuesta = userController.createUser(usuarioAEnviar);

        // --- 3. Assert ---
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        Map<String, String> responseBody = (Map<String, String>) respuesta.getBody();
        assertThat(responseBody.get("message")).isEqualTo("Error interno al crear el usuario");
    }

    @Test
    @DisplayName("Debería devolver 500 Internal Server Error al actualizar un usuario si ocurre un error inesperado")
    void updateUser_WhenUnexpectedError_ShouldReturnInternalServerError() {
        // --- 1. Arrange ---
        long userId = 1L;
        when(userService.updateUser(eq(userId), any(UserSec.class))).thenThrow(new RuntimeException("Error inesperado"));

        // --- 2. Act ---
        ResponseEntity<Object> respuesta = userController.updateUser(userId, new UserSec());

        // --- 3. Assert ---
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        Map<String, String> responseBody = (Map<String, String>) respuesta.getBody();
        assertThat(responseBody.get("message")).isEqualTo("Error inesperado al actualizar el usuario");
    }

    @Test
    @DisplayName("Debería devolver 500 Internal Server Error al eliminar un usuario si ocurre un error inesperado")
    void deleteUser_WhenUnexpectedError_ShouldReturnInternalServerError() {
        // --- 1. Arrange ---
        long userId = 1L;
        // Simulamos un error genérico (diferente de ResourceNotFoundException)
        doThrow(new RuntimeException("Error de base de datos simulado")).when(userService).deleteUser(userId);

        // --- 2. Act ---
        ResponseEntity<Map<String, String>> respuesta = userController.deleteUser(userId);

        // --- 3. Assert ---
        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        Map<String, String> responseBody = (Map<String, String>) respuesta.getBody();
        assertThat(responseBody.get("message")).isEqualTo("Error al eliminar el usuario");
    }
}