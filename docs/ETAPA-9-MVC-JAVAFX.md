# Etapa 9 — arquitectura MVC e interfaz JavaFX

Se incorpora un incremento visual sin retirar la consola: `ArenaSurApplication` inicia JavaFX, `InicioView` representa el catálogo, clientes y reservas, y `InicioController` coordina la lectura y altas JDBC mediante `ContextoAplicacionJdbc`.

La pantalla inicial permite cargar y actualizar canchas y tarifas, registrar clientes y registrar reservas pendientes. La pestaña de pagos y cambios permite registrar seña, saldo, modificación, cancelación y consultar movimientos, reutilizando los repositorios transaccionales y conservando la consola como camino alternativo.

Para ejecutar la interfaz se requiere configurar el archivo `.env` y utilizar `mvn javafx:run` o lanzar la clase `ar.arenasur.mvc.ArenaSurApplication` desde el IDE con las dependencias Maven disponibles.
