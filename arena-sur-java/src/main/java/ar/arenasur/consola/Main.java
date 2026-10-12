package ar.arenasur.consola;
import ar.arenasur.modelo.*;
import ar.arenasur.servicio.*;
import ar.arenasur.repositorio.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.*;
import java.util.*;

public final class Main {
    private static final Scanner ENTRADA = new Scanner(System.in);
    private static final DateTimeFormatter FORMATO =
        DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);
    public static ReservaService crearServicio(Clock reloj) {
        List<Cancha> canchas = List.of(new CanchaFutbol(1,"Fútbol 1"), new CanchaFutbol(2,"Fútbol 2"),
            new CanchaPadel(3,"Pádel 1"), new CanchaPadel(4,"Pádel 2"), new CanchaTenis(5,"Tenis 1"));
        List<Tarifa> tarifas = List.of(
            new Tarifa(1,"Fútbol 5",60,new BigDecimal("60000.00")),
            new Tarifa(2,"Fútbol 5",90,new BigDecimal("90000.00")),
            new Tarifa(3,"Pádel",60,new BigDecimal("55000.00")),
            new Tarifa(4,"Pádel",90,new BigDecimal("80000.00")),
            new Tarifa(5,"Tenis",60,new BigDecimal("40000.00")),
            new Tarifa(6,"Tenis",90,new BigDecimal("60000.00")));
        return new ReservaService(new ReservaMemoriaRepositorio(), reloj, canchas, tarifas);
    }
    private static String leer(String mensaje) {
        System.out.print(mensaje);
        if (!ENTRADA.hasNextLine()) throw new NoSuchElementException();
        return ENTRADA.nextLine().trim();
    }
    private static long id(String mensaje) { return Long.parseLong(leer(mensaje)); }
    private static LocalDateTime inicio() { return LocalDateTime.parse(leer("Inicio (AAAA-MM-DD HH:mm): "), FORMATO); }
    private static Long idOpcional(String mensaje) {
        String valor = leer(mensaje);
        return valor.isBlank() ? null : Long.valueOf(valor);
    }
    private static LocalDate fechaOpcional(String mensaje) {
        String valor = leer(mensaje);
        return valor.isBlank() ? null : LocalDate.parse(valor);
    }
    private static EstadoReserva estadoOpcional(String mensaje) {
        String valor = leer(mensaje);
        if (valor.isBlank()) return null;
        try { return EstadoReserva.valueOf(valor.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) {
            throw new NegocioException("Estado inválido. Use PENDIENTE, CONFIRMADA o CANCELADA.");
        }
    }
    private static String textoOpcional(String mensaje) {
        String valor = leer(mensaje);
        return valor.isBlank() ? null : valor;
    }
    public static void main(String[] args) {
        ReservaService servicio = crearServicio(Clock.system(ZoneId.of("America/Argentina/Buenos_Aires")));
        System.out.println("ARENA SUR - Prototipo en memoria. Los datos se pierden al salir.");
        boolean continuar = true;
        while (continuar) {
            System.out.println("\n1 Registrar cliente | 2 Listar clientes | 3 Canchas y tarifas");
            System.out.println("4 Consultar disponibilidad | 5 Registrar reserva | 6 Listar reservas");
            System.out.println("7 Registrar seña/diferencia | 8 Registrar saldo | 9 Consultar pagos | 10 Cancelar reserva");
            System.out.println("11 Modificar reserva | 12 Consultar reservas | 13 Consultar agenda | 0 Salir");
            try {
                int opcion = Integer.parseInt(leer("Opción: "));
                switch (opcion) {
                    case 0 -> continuar = false;
                    case 1 -> System.out.println(servicio.registrarCliente(leer("Nombre: "), leer("Apellido: "),
                        leer("Teléfono: "), leer("Email: ")));
                    case 2 -> servicio.clientes().forEach(System.out::println);
                    case 3 -> {
                        servicio.canchas().forEach(System.out::println);
                        servicio.tarifas().forEach(t -> System.out.println(t.deporte() + " | "
                            + t.duracion() + " minutos | $" + t.precio()));
                    }
                    case 4 -> {
                        long cancha = id("ID cancha: ");
                        LocalDateTime inicio = inicio();
                        int duracion = Integer.parseInt(leer("Duración (60/90): "));
                        System.out.println(servicio.disponible(cancha,inicio,duracion) ? "DISPONIBLE" : "OCUPADA");
                    }
                    case 5 -> {
                        long cliente = id("ID cliente: "), cancha = id("ID cancha: ");
                        LocalDateTime inicio = inicio();
                        int duracion = Integer.parseInt(leer("Duración (60/90): "));
                        System.out.println("Reserva registrada: " + servicio.registrar(cliente,cancha,inicio,duracion));
                    }
                    case 6 -> servicio.reservas().forEach(System.out::println);
                    case 7 -> {
                        Pago pago = servicio.registrarSena(id("ID reserva: "));
                        System.out.println("Pago de seña registrado: " + pago);
                    }
                    case 8 -> System.out.println("Saldo registrado: "
                        + servicio.registrarSaldo(id("ID reserva: ")));
                    case 9 -> {
                        long reserva = id("ID reserva: ");
                        servicio.pagos(reserva).forEach(System.out::println);
                        System.out.println("Saldo pendiente: $" + servicio.saldoPendiente(reserva));
                        System.out.println("Seña/diferencia pendiente: $" + servicio.diferenciaSenaPendiente(reserva));
                        System.out.println("Crédito a favor: $" + servicio.creditoFavor(reserva));
                    }
                    case 10 -> {
                        Reserva cancelada = servicio.cancelar(id("ID reserva: "));
                        System.out.println("Reserva cancelada: " + cancelada);
                        servicio.pagos(cancelada.getId()).stream()
                            .filter(p -> p.getTipo() == TipoPago.DEVOLUCION)
                            .forEach(System.out::println);
                    }
                    case 11 -> {
                        long reserva = id("ID reserva: "), cancha = id("Nuevo ID cancha: ");
                        LocalDateTime nuevoInicio = inicio();
                        int duracion = Integer.parseInt(leer("Nueva duración (60/90): "));
                        Reserva modificada = servicio.modificar(reserva, cancha, nuevoInicio, duracion);
                        System.out.println("Reserva modificada: " + modificada);
                        System.out.println("Seña/diferencia pendiente: $" + servicio.diferenciaSenaPendiente(reserva));
                        System.out.println("Crédito a favor: $" + servicio.creditoFavor(reserva));
                    }
                    case 12 -> {
                        System.out.println("Deje en blanco los criterios que no desea utilizar.");
                        Long cliente = idOpcional("ID cliente: ");
                        Long cancha = idOpcional("ID cancha: ");
                        LocalDate fecha = fechaOpcional("Fecha (AAAA-MM-DD): ");
                        EstadoReserva estado = estadoOpcional("Estado (PENDIENTE/CONFIRMADA/CANCELADA): ");
                        List<Reserva> resultados = servicio.consultarReservas(cliente, cancha, fecha, estado);
                        if (resultados.isEmpty()) {
                            System.out.println("No se encontraron reservas para los criterios ingresados.");
                        } else {
                            resultados.forEach(System.out::println);
                            String seleccion = leer("ID de reserva para ver el detalle (Enter para volver): ");
                            if (!seleccion.isBlank()) {
                                long reservaId = Long.parseLong(seleccion);
                                if (resultados.stream().noneMatch(r -> r.getId() == reservaId))
                                    throw new NegocioException("Seleccione un ID de los resultados.");
                                Reserva detalle = servicio.detalleReserva(reservaId);
                                System.out.println("Detalle: Reserva " + detalle.getId()
                                    + " | Cliente: " + detalle.getCliente().getNombreCompleto()
                                    + " | Cancha: " + detalle.getCancha().getNombre()
                                    + " (" + detalle.getCancha().getDeporte() + ")"
                                    + " | Inicio: " + detalle.getInicio() + " | Fin: " + detalle.getFin()
                                    + " | Duración: " + detalle.getDuracion() + " minutos"
                                    + " | Estado: " + detalle.getEstado()
                                    + " | Total: $" + detalle.getPrecioTotal()
                                    + " | Seña: $" + detalle.getSena()
                                    + (detalle.getEstado() == EstadoReserva.PENDIENTE
                                        ? " | Vence seña: " + detalle.getVencimiento() : ""));
                            }
                        }
                    }
                    case 13 -> {
                        LocalDate fecha = LocalDate.parse(leer("Fecha de agenda (AAAA-MM-DD): "));
                        Long canchaId = idOpcional("ID cancha (Enter para todas): ");
                        String deporte = textoOpcional("Deporte (Fútbol 5/Pádel/Tenis; Enter para todos): ");
                        AgendaDiaria agenda = servicio.consultarAgenda(fecha, canchaId, deporte);
                        System.out.println("AGENDA DEL " + fecha);
                        boolean hayOcupaciones = agenda.canchas().stream()
                            .anyMatch(a -> !a.ocupaciones().isEmpty());
                        if (!hayOcupaciones)
                            System.out.println("No hay turnos ocupados para los criterios ingresados.");
                        for (AgendaCancha agendaCancha : agenda.canchas()) {
                            Cancha cancha = agendaCancha.cancha();
                            System.out.println("\n" + cancha.getNombre() + " (" + cancha.getDeporte() + ")");
                            if (agendaCancha.ocupaciones().isEmpty()) {
                                System.out.println("Ocupaciones: ninguna.");
                            } else {
                                agendaCancha.ocupaciones().forEach(r -> System.out.println(
                                    "OCUPADA | " + r.getInicio().toLocalTime() + "–"
                                        + r.getFin().toLocalTime() + " | " + r.getDuracion() + " min"
                                        + " | " + r.getCliente().getNombreCompleto()
                                        + " | " + r.getEstado() + " | Reserva " + r.getId()));
                            }
                            for (int duracion : List.of(60, 90)) {
                                List<FranjaDisponible> franjas = agendaCancha.franjasDisponibles().stream()
                                    .filter(f -> f.duracion() == duracion).toList();
                                if (franjas.isEmpty()) {
                                    System.out.println("Sin inicios disponibles para " + duracion + " minutos.");
                                } else {
                                    franjas.forEach(f -> System.out.println("DISPONIBLE para " + duracion
                                        + " min | inicio entre " + f.desde().toLocalTime()
                                        + " y " + f.hasta().toLocalTime()));
                                }
                            }
                        }
                    }
                    default -> System.out.println("Opción inexistente.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Ingrese un número entero válido.");
            } catch (DateTimeParseException e) {
                System.out.println("Fecha inválida. Use AAAA-MM-DD en consultas o AAAA-MM-DD HH:mm para turnos.");
            } catch (NegocioException | IllegalArgumentException e) {
                System.out.println("No se pudo completar: " + e.getMessage());
            } catch (NoSuchElementException e) {
                continuar = false;
            }
        }
        System.out.println("Hasta luego.");
    }
}
