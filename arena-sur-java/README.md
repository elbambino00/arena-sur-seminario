# Arena Sur — primer incremento TP3

Aplicación de consola Java 17 o superior (compatible con Java 21). No requiere frameworks ni dependencias externas.

## Ejecutar en Windows

1. Extraer el ZIP.
2. Abrir PowerShell en la carpeta arena-sur-java.
3. Verificar que java y javac estén en PATH (requiere JDK, no solamente JRE).
4. Ejecutar:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\ejecutar.ps1
```

Para ejecutar las verificaciones:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\probar.ps1
```

En IntelliJ: abrir pom.xml, seleccionar JDK 21 y ejecutar ar.arenasur.consola.Main.

## Alcance y datos

CU02: registra y lista clientes.
CU03: consulta disponibilidad de una cancha, fecha, hora y duración.
CU04: registra una reserva pendiente, verifica cliente y cancha, calcula seña del 20% y fija vencimiento a 30 minutos.
CU05: modifica reservas pendientes o confirmadas, si faltan al menos dos horas para el turno original; valida disponibilidad y recalcula tarifa, seña, diferencia pendiente o crédito.
CU06: cancela reservas y registra devoluciones según el plazo.
CU08/CU09: registra señas, diferencias de seña y saldos, y consulta los movimientos.
CU07: consulta reservas mediante filtros combinables por cliente, cancha, fecha y estado; presenta detalle sin modificar los datos.
CU10: consulta la agenda por fecha, con filtros opcionales por deporte/cancha; muestra ocupaciones activas y ventanas de inicio disponibles para turnos de 60/90 minutos.

Las cinco canchas y seis tarifas reproducen los datos del SQL de AP2: fútbol 60/90 minutos $60.000/$90.000; pádel $55.000/$80.000; tenis $40.000/$60.000.
El operador es el usuario de demostración con ID 1, sin autenticación implementada.

Los datos están en memoria y se pierden al cerrar. El vencimiento se actualiza al consultar o registrar; no hay tarea en segundo plano.
No están implementadas autenticación ni conexión MySQL.
El horario de inicio permitido es de 09:00 a 22:00, ambos inclusive. Un turno iniciado a las 22:00 puede terminar a las 23:00 o 23:30. No permite turnos que crucen medianoche.
Es el primer incremento, no la entrega TP3 completa ni el documento académico final.

## Organización y POO

modelo: Cliente, Tarifa, Cancha, especializaciones y Reserva.
servicio: validaciones, disponibilidad y registro.
repositorio: contrato de persistencia e implementación ArrayList.
consola: lectura de datos, menú y tratamiento de errores.

Encapsulamiento: atributos privados y actualización de estado mediante método.
Herencia: CanchaFutbol, CanchaPadel y CanchaTenis extienden Cancha.
Polimorfismo: una List<Cancha> invoca getDeporte() de cada especialización; Tarifa mantiene el precio sin duplicarlo en subclases.
Abstracción: Cancha abstracta y ReservaRepositorio permiten ocultar detalles del deporte y de almacenamiento.
Constructores validan objetos; BigDecimal representa dinero; LocalDateTime representa turnos; enum representa estados.
El menú usa while, switch y try/catch; listas y búsquedas se realizan con List, ArrayList y streams.

## Demostración manual

1. Registrar un cliente (opción 1) y consultar su ID (2).
2. Listar canchas y tarifas (3).
3. Consultar un turno futuro de 60 minutos en cancha 1 (4): disponible.
4. Registrar ese turno (5): pendiente, total $60.000 y seña $12.000.
5. Consultar el mismo horario o uno superpuesto: ocupado.
6. Registrar la seña de la reserva (7): movimiento de pago y reserva confirmada.
7. Modificar una reserva (11): ingresar su ID, nueva cancha, fecha/hora y duración; verificar el nuevo resumen y cualquier diferencia o crédito.
8. Registrar el saldo (8) al finalizar el turno: antes de ese momento se rechaza; luego se calcula el importe pendiente.
9. Consultar reservas (12): ingresar al menos un filtro; se pueden combinar. Seleccionar un ID de los resultados para ver el detalle.
10. Consultar pagos (9) para ver movimientos, diferencia de seña pendiente, crédito y saldo pendiente.
11. Cancelar una reserva (10): con al menos 2 h de anticipación se devuelve lo abonado; más tarde se retiene la seña y se devuelve el saldo pagado.
12. Registrar el mismo turno: mensaje de error; un turno contiguo queda disponible.
13. Ingresar letras como opción o una fecha imposible: error controlado.
14. Consultar agenda (13): elegir fecha y, si se desea, filtrar por cancha o deporte. Revisar las ocupaciones y los rangos de inicio disponibles para 60 y 90 minutos.
15. Listar todas las reservas (6) y salir (0).

Tomar capturas reales de estos pasos para el documento académico. No se incluyen capturas simuladas.

## Pagos y cancelación

Registrar seña requiere una reserva pendiente dentro de los 30 minutos; registra el 20% una sola vez y confirma la reserva. El saldo (80%) se registra una sola vez, únicamente para reservas confirmadas y cuando la hora actual alcanza o supera el fin del turno.
La cancelación a dos horas o más devuelve todos los pagos realizados. Luego de ese límite se retiene la seña y se devuelve un saldo ya pagado. Cada devolución queda en el historial como movimiento DEVOLUCION. El estado cancelado libera la cancha.

La modificación aplica a reservas Pendientes y Confirmadas. Se permite hasta dos horas antes del inicio original; el horario nuevo debe ser futuro y estar disponible. Si la reserva estaba Pendiente, el plazo para pagar la seña vuelve a comenzar por 30 minutos. Si la seña requerida aumenta, se conserva la reserva Confirmada y queda pendiente la diferencia, registrable como un movimiento de seña adicional. Si disminuye, el excedente pagado se considera crédito a favor y se descuenta del saldo pendiente de esa misma reserva.

La consulta CU07 admite uno o más filtros: cliente, cancha, fecha y estado. Si se combinan criterios, se aplican todos simultáneamente. Los resultados se ordenan por fecha y hora de inicio; se puede solicitar el detalle de una reserva sin alterar su estado ni sus movimientos de pago. Para ver el listado completo permanece disponible la opción 6.

La agenda CU10 requiere una fecha y admite filtros opcionales por cancha y deporte, que se combinan si ambos se ingresan. Muestra reservas Pendientes vigentes y Confirmadas, excluye Canceladas y ordena ocupaciones por cancha y hora. Para cada cancha informa ventanas de inicio disponibles por duración (60/90 minutos), entre las 09:00 y las 22:00 inclusive. El vencimiento de una reserva Pendiente se actualiza al consultar la agenda y libera el horario.

## Continuidad con AP2

Se conserva la separación Cancha/Tarifa y la referencia de cliente, tarifa y operador en reservas.
El modelo Java agrega la jerarquía de canchas para evidenciar POO y permite modificar cancha, horario y duración de una reserva.
La clase Reserva conserva precio/seña históricos en forma derivada de una Tarifa inmutable.
Para TP4 incorporar un repositorio JDBC con transacciones, control de concurrencia y actualización persistente del vencimiento.
El contrato actual devuelve objetos mutables: al incorporar JDBC debe explicitarse una operación de actualización de estado.
No se modifica el SQL de AP2 ni se publica este código automáticamente en GitHub.
