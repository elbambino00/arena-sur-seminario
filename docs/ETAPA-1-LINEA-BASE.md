# Etapa 1 — Línea base de Arena Sur

Fecha de verificación: 2 de noviembre de 2026.

## Resultado

La implementación existente compila y sus 78 verificaciones automatizadas finalizan correctamente. En esta etapa no se modificaron reglas de negocio ni código Java.

## Entorno verificado

- JDK: Eclipse Temurin 21.0.12.1 LTS.
- Nivel de compilación actual: Java 17 (`--release 17` y propiedades Maven 17).
- Maven disponible: 3.9.16.
- Aplicación: consola Java sin dependencias externas.
- Persistencia actual: repositorios en memoria basados en `ArrayList`.
- Persistencia MySQL: todavía no integrada.

## Comando reproducible

Desde `arena-sur-java`:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\probar.ps1
```

El uso de `ExecutionPolicy Bypass` es necesario en el equipo de desarrollo porque la política local impide ejecutar scripts de PowerShell sin firma.

## Cobertura funcional comprobada

- Registro y consulta de clientes.
- Disponibilidad y prevención de superposiciones.
- Alta, modificación, consulta, vencimiento y cancelación de reservas.
- Seña, diferencia de seña, saldo, crédito y devoluciones.
- Filtros combinables y detalle de reservas (CU07).
- Agenda diaria, filtros y franjas disponibles (CU10).
- Validaciones de horarios, duraciones, estados y duplicados.

La salida completa actualizada se conserva en `arena-sur-java/VERIFICACION.txt`.

## Estado del espacio de trabajo

La carpeta `ArenaSur` está vinculada al repositorio `https://github.com/elbambino00/arena-sur-seminario.git`. La rama local es `main` y toma como base `origin/main`. No se realizó ningún commit ni push.

## Hallazgos para las próximas etapas

1. El script SQL de AP2 tiene un error de sintaxis: falta el punto y coma después de `COLLATE utf8mb4_0900_ai_ci`.
2. No se pudo validar el esquema contra un servidor local porque no hay un cliente MySQL accesible en el entorno actual.
3. `pom.xml` compila para Java 17; la migración explícita a Java 21 corresponde a la etapa de configuración Java.
4. `VERIFICACION.txt` informaba 64 pruebas y no incluía los 14 escenarios de CU10; quedó actualizado a las 78 verificaciones reales.

## Próximo hito

La etapa 2 debe estabilizar el script MySQL: corregir su sintaxis, hacerlo reproducible, validar tablas, restricciones, índices y datos iniciales, y documentar cómo ejecutarlo en un servidor MySQL 8.
