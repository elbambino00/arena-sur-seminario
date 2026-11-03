# Etapa 7 — ciclo de vida transaccional de reservas

Esta etapa completa las operaciones que ocurren después de crear una reserva:

- modificación de cancha, duración y horario, conservando el estado y recalculando precio y seña;
- cobro del saldo únicamente cuando finalizó el turno y la seña está completa;
- cancelación con devolución total de lo abonado si se realiza con al menos dos horas de anticipación;
- cancelación tardía con retención de la seña y devolución de los pagos de saldo;
- bloqueo pesimista de la reserva y transacciones JDBC para evitar pagos duplicados o cambios parciales.

La modificación recalcula también el crédito a favor cuando la seña ya abonada supera la nueva seña. Todas las operaciones registran sus movimientos en `pago` y dejan la reserva persistida antes de devolver el resultado.

## Verificación

Las pruebas de negocio existentes se ejecutan con:

```powershell
.\probar.ps1
```

La integración JDBC de reservas se ejecuta con un MySQL configurado mediante `ARENA_SUR_DB_URL`, `ARENA_SUR_DB_USER` y `ARENA_SUR_DB_PASSWORD`:

```powershell
.\verificar-reservas-jdbc.ps1
```
