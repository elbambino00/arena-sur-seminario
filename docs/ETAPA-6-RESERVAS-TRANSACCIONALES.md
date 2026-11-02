# Etapa 6 — Reservas y señas transaccionales

Fecha de verificación: 2 de noviembre de 2026.

## Resultado

Arena Sur puede registrar reservas pendientes y cobrar su seña mediante transacciones JDBC. La disponibilidad se controla dentro de la misma transacción que realiza la inserción, y los intentos concurrentes sobre una cancha se serializan correctamente.

## Modelo persistente

Se agregó `SolicitudReserva` para validar los datos de una reserva nueva antes de abrir la transacción.

`Reserva` ahora puede rehidratarse desde MySQL conservando:

- Precio histórico.
- Importe histórico de la seña.
- Crédito a favor.
- Estado.
- Fecha de creación y vencimiento.

Esto evita que un cambio futuro de tarifa altere reservas anteriores.

## Límite transaccional

`ReservaTransaccionalRepositorio` concentra la persistencia de la reserva y sus pagos relacionados. `JdbcReservaTransaccionalRepositorio` implementa:

- Registro de una reserva pendiente.
- Búsqueda y listado completo.
- Registro atómico de la seña.
- Confirmación de la reserva dentro de la misma transacción.
- Consulta de pagos.
- Persistencia del vencimiento cuando se intenta pagar fuera de término.

## Control de concurrencia

Antes de comprobar superposiciones, la transacción bloquea con `SELECT ... FOR UPDATE` la fila de la cancha. Todos los registros de reserva de esta implementación respetan el mismo bloqueo.

Mientras mantiene el bloqueo:

1. Cancela pendientes vencidas para esa cancha y fecha.
2. Consulta intervalos activos superpuestos.
3. Inserta únicamente si el horario continúa disponible.
4. Confirma la transacción.

La prueba lanzó simultáneamente dos intentos para la misma cancha y horario. Uno se confirmó y el otro recibió `ConflictoReservaException`, sin duplicar la ocupación.

## Seña

El cobro bloquea la reserva, valida estado y vencimiento, suma señas anteriores, inserta el movimiento y cambia el estado a `CONFIRMADA` antes de confirmar la transacción. Un segundo intento no genera otro pago.

## Verificación

- 86 verificaciones de negocio aprobadas.
- 11 verificaciones JDBC del catálogo aprobadas.
- 8 verificaciones JDBC de clientes aprobadas.
- 13 verificaciones JDBC transaccionales aprobadas.
- Compilación Maven con Java 21 aprobada.
- Limpieza confirmada: permanecen únicamente las 2 reservas y el pago inicial del esquema.

## Alcance pendiente

Todavía falta trasladar a la capa JDBC:

- Modificación de reservas y diferencias de seña.
- Registro del saldo.
- Cancelación anticipada o tardía y devoluciones.
- Integración del flujo persistente con la consola.

## Próximo hito

La etapa 7 debe completar esas transiciones dentro de transacciones y cubrirlas con pruebas de integración antes de activar MySQL en la aplicación principal.
