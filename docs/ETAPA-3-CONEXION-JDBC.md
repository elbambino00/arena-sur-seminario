# Etapa 3 — Java 21 y conexión JDBC

Fecha de verificación: 2 de noviembre de 2026.

## Resultado

Arena Sur compila con Java 21 y puede conectarse correctamente a MySQL mediante el controlador oficial Connector/J. La prueba de integración se realizó contra MySQL Community Server 8.0.46 con un usuario de aplicación separado de `root`.

## Configuración técnica

- Java: Eclipse Temurin JDK 21.0.12.1.
- Maven: 3.9.16.
- Maven Compiler Plugin: 3.16.0.
- MySQL Connector/J: 26.10.0.
- Maven Dependency Plugin: 3.11.0.

El proyecto utiliza `maven.compiler.release=21`. Los scripts directos de compilación también usan `javac --release 21`.

## Seguridad de configuración

Las credenciales no están incluidas en el código ni en el `pom.xml`. La conexión requiere estas variables de entorno:

- `ARENA_SUR_DB_URL`
- `ARENA_SUR_DB_USER`
- `ARENA_SUR_DB_PASSWORD`

El archivo `.env.example` contiene únicamente nombres y valores ilustrativos. Los archivos `.env` reales están ignorados por Git.

## Componentes agregados

- `ConfiguracionBaseDatos`: valida y encapsula las variables de entorno.
- `ConexionMySql`: crea conexiones JDBC desde un único punto.
- `VerificarConexion`: diagnóstico manual que consulta el servidor y la base activa sin imprimir credenciales.
- `verificar-conexion.ps1`: compila con Maven, prepara las dependencias y ejecuta el diagnóstico.

## Verificaciones

- Compilación principal y de pruebas con Java 21: correcta.
- Resolución de Connector/J mediante Maven: correcta.
- Conexión con usuario `arena_sur_app`: correcta.
- Servidor detectado: MySQL 8.0.46.
- Base detectada: `arena_sur`.
- Verificaciones automatizadas: 81 aprobadas.

## Alcance actual

La conexión JDBC ya está preparada, pero la lógica funcional continúa usando repositorios en memoria. Esto permite comprobar la infraestructura sin alterar todavía las reglas de negocio existentes.

## Próximo hito

La etapa 4 debe implementar repositorios JDBC de lectura para deportes, canchas y tarifas, manteniendo los contratos actuales y agregando pruebas de integración contra MySQL.
