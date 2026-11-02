# Etapa 4 — Catálogo JDBC

Fecha de verificación: 2 de noviembre de 2026.

## Resultado

Arena Sur puede consultar desde MySQL los deportes, canchas y tarifas activos. La capa JDBC se integra con el modelo y con `ReservaService` sin reemplazar todavía la persistencia en memoria de clientes, reservas y pagos.

## Contratos agregados

- `DeporteRepositorio`: listado y búsqueda de deportes activos.
- `CanchaRepositorio`: listado y búsqueda de canchas activas.
- `TarifaRepositorio`: listado y búsqueda de tarifas activas por deporte y duración.

Los contratos no dependen de JDBC y permiten mantener separadas la lógica de negocio y la tecnología de persistencia.

## Implementaciones

- `JdbcDeporteRepositorio`.
- `JdbcCanchaRepositorio`.
- `JdbcTarifaRepositorio`.

Todas las consultas:

- Usan `PreparedStatement`.
- Abren y cierran conexiones, consultas y resultados mediante `try-with-resources`.
- Devuelven listas inmutables u objetos `Optional`.
- Filtran registros inactivos.
- Encapsulan `SQLException` dentro de `PersistenciaException`.

## Mapeo del modelo

Se agregó la entidad inmutable `Deporte`. Las canchas recuperadas se convierten según su deporte en `CanchaFutbol`, `CanchaPadel` o `CanchaTenis`, preservando el polimorfismo existente. Las tarifas conservan `BigDecimal` para evitar pérdida de precisión.

## Verificación

La prueba se ejecutó contra MySQL Community Server 8.0.46 con un usuario de aplicación sin utilizar `root` desde Java.

- 81 verificaciones de negocio aprobadas.
- 11 verificaciones de integración JDBC aprobadas.
- Compilación Maven con Java 21 aprobada.

La integración comprobó cantidades, búsquedas por ID, resultados inexistentes, jerarquía de canchas, precio de tarifas y compatibilidad del catálogo persistente con `ReservaService`.

## Alcance deliberado

La consola continúa iniciando con el catálogo y los repositorios en memoria. Esto evita una migración parcial difícil de diagnosticar. La activación de MySQL en el flujo principal se realizará cuando clientes, reservas y pagos dispongan de implementaciones JDBC coordinadas.

## Próximo hito

La etapa 5 debe implementar persistencia JDBC de clientes y sus validaciones de unicidad. Después podrá abordarse el flujo transaccional de reservas y pagos.
