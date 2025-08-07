# =================================================================
# ETAPA 1: Construcción con Maven (Build Stage)
# Usamos una imagen que ya tiene Maven y Java para compilar.
# =================================================================
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app

# 1. Copia solo el pom.xml para aprovechar el cache de Docker.
COPY pom.xml .
# 2. Descarga las dependencias. Esta capa solo se reconstruirá si el pom.xml cambia.
RUN mvn dependency:go-offline
# 3. Copia el código fuente.
COPY src ./src
# 4. Empaqueta la aplicación. Las dependencias ya están cacheadas.
RUN mvn clean package -DskipTests
# =================================================================
# ETAPA 2: Ejecución (Final Stage)
# Usamos una imagen ligera solo con Java para ejecutar la app.
# =================================================================
FROM openjdk:17-jdk-slim
# Copiamos únicamente el .jar compilado desde la etapa anterior.
COPY --from=build /app/target/blogweb1-0.0.1.jar app_blogweb1.jar

# Instalamos el cliente de postgres y copiamos el script de espera
RUN apt-get update && apt-get install -y postgresql-client && rm -rf /var/lib/apt/lists/*
COPY wait-for-it.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/wait-for-it.sh


EXPOSE 8080
# El ENTRYPOINT se define en docker-compose.yml para poder usar el script de espera.
# CMD define el comando por defecto que el entrypoint ejecutará.
CMD ["java", "-jar", "app_blogweb1.jar"]