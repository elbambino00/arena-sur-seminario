# Base de datos de Arena Sur

El archivo `SCIANDRO-ESTEBAN-AP2.sql` crea el esquema MySQL 8, sus restricciones, índices y datos de demostración.

## Advertencia

Es un script de recreación para desarrollo: elimina y vuelve a crear exclusivamente la base `arena_sur`. No debe ejecutarse sobre una base con información que se quiera conservar.

## Ejecución con MySQL instalado

Desde la raíz del repositorio:

```powershell
Get-Content -Raw -Encoding UTF8 .\database\SCIANDRO-ESTEBAN-AP2.sql | mysql -u root -p
Get-Content -Raw -Encoding UTF8 .\database\validar_arena_sur.sql | mysql -u root -p
```

## Ejecución aislada con Docker

```powershell
docker run --name arena-sur-mysql `
  -e MYSQL_ROOT_PASSWORD=arena_sur_dev `
  -p 3307:3306 `
  -d mysql:8.0

Get-Content -Raw -Encoding UTF8 .\database\SCIANDRO-ESTEBAN-AP2.sql |
  docker exec -i arena-sur-mysql mysql -uroot -parena_sur_dev

Get-Content -Raw -Encoding UTF8 .\database\validar_arena_sur.sql |
  docker exec -i arena-sur-mysql mysql -uroot -parena_sur_dev
```

La contraseña anterior es únicamente para el contenedor local de desarrollo. No debe reutilizarse en otros entornos ni incorporarse a la futura configuración JDBC.

## Resultado esperado

- 7 tablas: `deporte`, `cliente`, `usuario`, `cancha`, `tarifa`, `reserva` y `pago`.
- 3 deportes.
- 5 canchas.
- 6 tarifas.
- 3 clientes de demostración.
- 2 usuarios de demostración.
- 2 reservas.
- 1 pago.

El script puede ejecutarse nuevamente y produce el mismo estado inicial porque recrea la base completa.

## Integridad y concurrencia

El esquema valida claves foráneas, valores monetarios, duraciones permitidas, unicidad de datos maestros y coherencia temporal del vencimiento. La superposición de reservas no puede resolverse con una restricción `UNIQUE` porque trabaja con intervalos; deberá controlarse en la futura capa JDBC mediante una transacción y bloqueo de las reservas activas de la cancha.
