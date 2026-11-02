# Etapa 2 — Esquema MySQL 8

Fecha de verificación: 2 de noviembre de 2026.

## Resultado

El esquema de Arena Sur fue corregido y validado en MySQL Community Server 8.0.46. El script se ejecutó dos veces consecutivas y produjo el mismo estado inicial, sin errores ni datos duplicados.

## Cambios realizados

- Se corrigió el punto y coma faltante en la creación de la base.
- El script recrea de forma explícita la base de desarrollo `arena_sur`.
- Se documentó el comportamiento destructivo para evitar su uso accidental sobre datos reales.
- Se agregó unicidad para nombres de canchas y combinaciones deporte/duración de tarifas.
- Se reforzaron las validaciones de seña, crédito y vencimiento.
- Se agregó un script independiente de comprobación del esquema y los datos.
- Se documentaron alternativas de ejecución con MySQL instalado y con Docker.

## Validaciones ejecutadas

Todas finalizaron con resultado `OK`:

- 7 tablas.
- 7 claves primarias.
- 5 restricciones únicas.
- 8 claves foráneas.
- 8 restricciones `CHECK`.
- 4 índices de consulta.
- Cantidades esperadas de deportes, canchas, tarifas, clientes, usuarios, reservas y pagos.
- Ausencia de relaciones huérfanas.
- Correspondencia entre la seña registrada y la reserva confirmada.

## Archivos

- `database/SCIANDRO-ESTEBAN-AP2.sql`: recreación completa del esquema y datos de demostración.
- `database/validar_arena_sur.sql`: controles posteriores a la creación.
- `database/README.md`: instrucciones y advertencias de ejecución.

## Límite conocido

MySQL no puede impedir por medio de una restricción `UNIQUE` la superposición de intervalos horarios. La futura implementación JDBC deberá registrar reservas dentro de una transacción y bloquear las reservas activas de la cancha mientras verifica disponibilidad e inserta el nuevo turno.

## Próximo hito

La etapa 3 debe configurar Java 21 y MySQL Connector/J, externalizar las credenciales y comprobar una conexión JDBC sin reemplazar todavía los repositorios en memoria.
