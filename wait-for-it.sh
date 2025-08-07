#!/bin/sh
# wait-for-it.sh
# Este script espera a que la base de datos PostgreSQL esté lista antes de ejecutar el comando principal.

set -e

# El primer argumento es el host de la base de datos (ej: postgres-db)
host="$1"
shift

# Bucle que se ejecuta hasta que psql puede conectarse exitosamente.
# Utiliza las variables de entorno que son pasadas por docker-compose.
until PGPASSWORD=$POSTGRES_PASSWORD psql -h "$host" -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c '\q'; do
  >&2 echo "Postgres no está disponible todavía - esperando..."
  sleep 1
done

>&2 echo "¡Postgres está listo! - Ejecutando el comando principal."
# exec "$@" es la forma correcta y segura de ejecutar el resto de los argumentos que se pasaron al script.
exec "$@"