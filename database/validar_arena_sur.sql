-- Validación posterior a SCIANDRO-ESTEBAN-AP2.sql.
-- Todas las filas de la columna resultado deben mostrar OK.

USE arena_sur;

SELECT 'tablas' AS verificacion,
       IF(COUNT(*) = 7, 'OK', CONCAT('ERROR: ', COUNT(*))) AS resultado
FROM information_schema.tables
WHERE table_schema = 'arena_sur'
  AND table_type = 'BASE TABLE'
UNION ALL
SELECT 'claves_primarias', IF(COUNT(*) = 7, 'OK', CONCAT('ERROR: ', COUNT(*)))
FROM information_schema.table_constraints
WHERE constraint_schema = 'arena_sur'
  AND constraint_type = 'PRIMARY KEY'
UNION ALL
SELECT 'restricciones_unicas', IF(COUNT(*) = 5, 'OK', CONCAT('ERROR: ', COUNT(*)))
FROM information_schema.table_constraints
WHERE constraint_schema = 'arena_sur'
  AND constraint_type = 'UNIQUE'
UNION ALL
SELECT 'claves_foraneas', IF(COUNT(*) = 8, 'OK', CONCAT('ERROR: ', COUNT(*)))
FROM information_schema.table_constraints
WHERE constraint_schema = 'arena_sur'
  AND constraint_type = 'FOREIGN KEY'
UNION ALL
SELECT 'restricciones_check', IF(COUNT(*) = 8, 'OK', CONCAT('ERROR: ', COUNT(*)))
FROM information_schema.table_constraints
WHERE constraint_schema = 'arena_sur'
  AND constraint_type = 'CHECK'
UNION ALL
SELECT 'indices_consulta', IF(COUNT(DISTINCT index_name) = 4, 'OK', CONCAT('ERROR: ', COUNT(DISTINCT index_name)))
FROM information_schema.statistics
WHERE table_schema = 'arena_sur'
  AND index_name LIKE 'idx_%'
UNION ALL
SELECT 'deportes', IF(COUNT(*) = 3, 'OK', CONCAT('ERROR: ', COUNT(*))) FROM deporte
UNION ALL
SELECT 'canchas', IF(COUNT(*) = 5, 'OK', CONCAT('ERROR: ', COUNT(*))) FROM cancha
UNION ALL
SELECT 'tarifas', IF(COUNT(*) = 6, 'OK', CONCAT('ERROR: ', COUNT(*))) FROM tarifa
UNION ALL
SELECT 'clientes', IF(COUNT(*) = 3, 'OK', CONCAT('ERROR: ', COUNT(*))) FROM cliente
UNION ALL
SELECT 'usuarios', IF(COUNT(*) = 2, 'OK', CONCAT('ERROR: ', COUNT(*))) FROM usuario
UNION ALL
SELECT 'reservas', IF(COUNT(*) = 2, 'OK', CONCAT('ERROR: ', COUNT(*))) FROM reserva
UNION ALL
SELECT 'pagos', IF(COUNT(*) = 1, 'OK', CONCAT('ERROR: ', COUNT(*))) FROM pago;

SELECT 'relaciones_huerfanas' AS verificacion,
       IF(COUNT(*) = 0, 'OK', CONCAT('ERROR: ', COUNT(*))) AS resultado
FROM (
    SELECT r.id_reserva
    FROM reserva r
    LEFT JOIN cliente c ON c.id_cliente = r.id_cliente
    LEFT JOIN cancha ca ON ca.id_cancha = r.id_cancha
    LEFT JOIN tarifa t ON t.id_tarifa = r.id_tarifa
    LEFT JOIN usuario u ON u.id_usuario = r.id_usuario
    WHERE c.id_cliente IS NULL
       OR ca.id_cancha IS NULL
       OR t.id_tarifa IS NULL
       OR u.id_usuario IS NULL

    UNION ALL

    SELECT p.id_pago
    FROM pago p
    LEFT JOIN reserva r ON r.id_reserva = p.id_reserva
    LEFT JOIN usuario u ON u.id_usuario = p.id_usuario
    WHERE r.id_reserva IS NULL
       OR u.id_usuario IS NULL
) AS huerfanos;

SELECT 'sena_confirmada' AS verificacion,
       IF(COUNT(*) = 1, 'OK', CONCAT('ERROR: ', COUNT(*))) AS resultado
FROM reserva r
JOIN pago p ON p.id_reserva = r.id_reserva
WHERE r.estado = 'CONFIRMADA'
  AND p.tipo = 'SENA'
  AND p.importe = r.importe_sena;
