# Blog Web con Spring Boot y Docker

Este es un proyecto de aplicación web de blog construida con Spring Boot, Spring Security, JPA/Hibernate y PostgreSQL. Todo el entorno está completamente dockerizado para un despliegue y desarrollo sencillos.

## Características

- **Backend:** Spring Boot 3
- **Seguridad:** Spring Security (autenticación con JWT y login social con GitHub OAuth2)
- **Base de Datos:** PostgreSQL
- **Contenerización:** Docker y Docker Compose
- **Build Tool:** Maven

---

## 🚀 Cómo Ejecutar el Proyecto con Docker (Recomendado)

La forma más sencilla de levantar toda la aplicación, incluyendo la base de datos.

### Prerrequisitos

- [Docker](https://www.docker.com/get-started)
- [Docker Compose](https://docs.docker.com/compose/install/)

### Pasos

1.  **Clona el repositorio:**
    ```sh
    git clone <URL-de-tu-repositorio>
    cd blogweb1
    ```

2.  **Crea el archivo de secretos `.env`:**
    En la raíz del proyecto, crea un archivo llamado `.env`. Puedes copiar la plantilla de abajo y rellenarla con tus propios secretos.

    ```
    # Credenciales para la Base de Datos PostgreSQL
    POSTGRES_DB=blogdb_docker
    POSTGRES_USER=adrian
    POSTGRES_PASSWORD=un_password_fuerte_y_seguro

    # Secretos de la Aplicación
    JWT_SECRET=un_secreto_muy_largo_y_dificil_de_adivinar_para_firmar_tokens
    JWT_USER=jwt_user_docker
    SS_USER=admin_docker
    SS_PASSWORD=admin_pass_docker

    # Credenciales de GitHub OAuth2 (obtenidas de tu App en GitHub)
    GITHUB_CLIENT_ID=tu_client_id_de_github
    GITHUB_CLIENT_SECRET=tu_client_secret_de_github
    ```

3.  **Levanta los contenedores:**
    Este comando construirá la imagen de la aplicación y levantará tanto el backend como la base de datos.
    ```sh
    docker-compose up --build
    ```

4.  **¡Listo!** La aplicación estará disponible en `http://localhost:8081`.

---