# Etapa 8 — integración de la aplicación

Se incorpora un punto de entrada JDBC independiente para validar la lectura conjunta del catálogo, clientes y reservas persistidas. Esto permite verificar la configuración completa de la aplicación antes de migrar las opciones del menú que todavía dependen del servicio en memoria.

La migración del menú se mantiene separada por responsabilidad: primero se valida el acceso persistente de cada agregado y luego se reemplaza el servicio de aplicación, evitando mezclar cambios de interfaz con cambios transaccionales.
