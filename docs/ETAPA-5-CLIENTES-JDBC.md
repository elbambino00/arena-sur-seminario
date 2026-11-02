# Etapa 5 — Clientes JDBC

Fecha de verificación: 2 de noviembre de 2026.

## Resultado

Arena Sur puede registrar y consultar clientes en MySQL. La implementación utiliza IDs autogenerados, respeta el estado activo y traduce la restricción única del email a una excepción comprensible para la aplicación.

## Modelo

Se agregó `DatosCliente`, un valor inmutable que valida y normaliza los datos antes de intentar la inserción. `Cliente` representa exclusivamente una entidad ya identificada y reutiliza esas mismas validaciones.

También se agregaron accesores individuales para nombre y apellido, necesarios para mapear la entidad sin depender del texto combinado de presentación.

## Contrato e implementación

`ClienteRepositorio` permite:

- Registrar un cliente y obtener su ID generado.
- Listar clientes activos.
- Buscar un cliente activo por ID.
- Buscar un cliente activo por email.

`JdbcClienteRepositorio` implementa el contrato mediante `PreparedStatement`, `RETURN_GENERATED_KEYS` y cierre automático de recursos.

## Unicidad

MySQL continúa siendo la autoridad definitiva sobre el email único. Esto evita una condición de carrera entre una consulta previa y la inserción. El código identifica el error MySQL 1062 y lo convierte en `RegistroDuplicadoException` con el mensaje `El email ya está registrado.`.

La comparación utiliza la collation `utf8mb4_0900_ai_ci`, por lo que también rechaza el mismo email con diferencias de mayúsculas.

## Verificación

- 83 verificaciones de negocio aprobadas.
- 11 verificaciones JDBC del catálogo aprobadas.
- 8 verificaciones JDBC de clientes aprobadas.
- Compilación Maven con Java 21 aprobada.

La prueba de integración registró un cliente, verificó el ID generado, las búsquedas, el listado, el duplicado y el filtrado por estado. Finalmente eliminó el dato temporal y confirmó que la base volvió a sus tres clientes iniciales.

## Alcance deliberado

La consola todavía utiliza su lista de clientes en memoria. La implementación JDBC queda preparada para conectarse al servicio cuando reservas y pagos también sean persistentes, evitando combinar entidades de orígenes distintos dentro del mismo flujo.

## Próximo hito

La etapa 6 debe implementar reservas y pagos JDBC dentro de transacciones. El primer objetivo será registrar una reserva pendiente con control de superposición concurrente y persistir el vencimiento de la seña.
