package ar.arenasur.servicio;
import ar.arenasur.modelo.*;
import ar.arenasur.repositorio.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** La validación y el registro pertenecen al servicio, no al menú. */
public final class ReservaService {
    private final ReservaRepositorio repositorio;
    private final Clock reloj;
    private final List<Cancha> canchas;
    private final List<Tarifa> tarifas;
    private final List<Cliente> clientes = new ArrayList<>();
    private final PagoRepositorio pagos;
    private long idPago = 1;
    public ReservaService(ReservaRepositorio repositorio, Clock reloj, List<Cancha> canchas, List<Tarifa> tarifas) {
        this(repositorio, new PagoMemoriaRepositorio(), reloj, canchas, tarifas);
    }
    public ReservaService(ReservaRepositorio repositorio, PagoRepositorio pagos, Clock reloj,
                          List<Cancha> canchas, List<Tarifa> tarifas) {
        this.repositorio = Objects.requireNonNull(repositorio); this.reloj = Objects.requireNonNull(reloj);
        this.pagos = Objects.requireNonNull(pagos);
        this.canchas = List.copyOf(canchas); this.tarifas = List.copyOf(tarifas);
    }
    public Cliente registrarCliente(String nombre, String apellido, String telefono, String email) {
        Cliente nuevo = new Cliente(clientes.size() + 1L, nombre, apellido, telefono, email);
        if (clientes.stream().anyMatch(c -> c.getEmail().equalsIgnoreCase(nuevo.getEmail())))
            throw new NegocioException("El email ya está registrado.");
        clientes.add(nuevo); return nuevo;
    }
    public List<Cliente> clientes() { return List.copyOf(clientes); }
    public List<Cancha> canchas() { return canchas; }
    public List<Tarifa> tarifas() { return tarifas; }
    public List<Reserva> reservas() {
        LocalDateTime ahora = LocalDateTime.now(reloj);
        List<Reserva> lista = repositorio.listar();
        lista.forEach(r -> r.actualizarVencimiento(ahora));
        return lista;
    }
    public List<Reserva> consultarReservas(Long clienteId, Long canchaId, LocalDate fecha,
                                           EstadoReserva estado) {
        if (clienteId == null && canchaId == null && fecha == null && estado == null)
            throw new NegocioException("Ingrese al menos un criterio de búsqueda.");
        if (clienteId != null && clientes.stream().noneMatch(c -> c.getId() == clienteId))
            throw new NegocioException("Cliente inexistente.");
        if (canchaId != null) cancha(canchaId);
        return repositorio.listar().stream()
            .filter(r -> clienteId == null || r.getCliente().getId() == clienteId)
            .filter(r -> canchaId == null || r.getCancha().getId() == canchaId)
            .filter(r -> fecha == null || r.getInicio().toLocalDate().equals(fecha))
            .filter(r -> estado == null || r.getEstado() == estado)
            .sorted(Comparator.comparing(Reserva::getInicio).thenComparingLong(Reserva::getId))
            .toList();
    }
    public AgendaDiaria consultarAgenda(LocalDate fecha, Long canchaId, String deporte) {
        if (fecha == null) throw new NegocioException("La fecha de consulta es obligatoria.");
        Cancha canchaSeleccionada = canchaId == null ? null : cancha(canchaId);
        String deporteNormalizado = deporte == null || deporte.isBlank() ? null : deporte.trim();
        if (deporteNormalizado != null && canchas.stream().noneMatch(c ->
                c.getDeporte().equalsIgnoreCase(deporteNormalizado)))
            throw new NegocioException("Deporte inexistente. Use Fútbol 5, Pádel o Tenis.");
        if (canchaSeleccionada != null && deporteNormalizado != null
                && !canchaSeleccionada.getDeporte().equalsIgnoreCase(deporteNormalizado))
            throw new NegocioException("La cancha no corresponde al deporte seleccionado.");

        List<Reserva> ocupacionesFecha = reservas().stream()
            .filter(r -> r.getInicio().toLocalDate().equals(fecha))
            .filter(r -> r.getEstado() == EstadoReserva.PENDIENTE
                || r.getEstado() == EstadoReserva.CONFIRMADA)
            .filter(r -> canchaSeleccionada == null || r.getCancha().getId() == canchaSeleccionada.getId())
            .filter(r -> deporteNormalizado == null
                || r.getCancha().getDeporte().equalsIgnoreCase(deporteNormalizado))
            .sorted(Comparator.comparing((Reserva r) -> r.getCancha().getId())
                .thenComparing(Reserva::getInicio).thenComparingLong(Reserva::getId))
            .toList();

        List<Cancha> canchasAgenda = canchas.stream()
            .filter(c -> canchaSeleccionada == null || c.getId() == canchaSeleccionada.getId())
            .filter(c -> deporteNormalizado == null || c.getDeporte().equalsIgnoreCase(deporteNormalizado))
            .sorted(Comparator.comparingLong(Cancha::getId)).toList();
        List<AgendaCancha> resultado = canchasAgenda.stream().map(c -> {
            List<Reserva> ocupaciones = ocupacionesFecha.stream()
                .filter(r -> r.getCancha().getId() == c.getId()).toList();
            return new AgendaCancha(c, ocupaciones, franjasDisponibles(fecha, c, ocupaciones));
        }).toList();
        return new AgendaDiaria(fecha, resultado);
    }
    private List<FranjaDisponible> franjasDisponibles(LocalDate fecha, Cancha cancha,
                                                       List<Reserva> ocupaciones) {
        LocalDateTime apertura = fecha.atTime(9, 0);
        LocalDateTime ultimoInicio = fecha.atTime(22, 0);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (fecha.equals(ahora.toLocalDate())) {
            LocalDateTime primerMinutoFuturo = ahora.plusMinutes(1).withSecond(0).withNano(0);
            if (primerMinutoFuturo.isAfter(apertura)) apertura = primerMinutoFuturo;
        }
        List<FranjaDisponible> franjas = new ArrayList<>();
        for (int duracion : List.of(60, 90)) {
            LocalDateTime proximoInicio = apertura;
            for (Reserva reserva : ocupaciones) {
                LocalDateTime inicioOcupado = reserva.getInicio();
                LocalDateTime finOcupado = reserva.getFin();
                if (inicioOcupado.isAfter(proximoInicio)) {
                    agregarFranja(franjas, cancha, duracion, proximoInicio,
                        inicioOcupado.minusMinutes(duracion).isBefore(ultimoInicio)
                            ? inicioOcupado.minusMinutes(duracion) : ultimoInicio);
                }
                if (finOcupado.isAfter(proximoInicio)) proximoInicio = finOcupado;
            }
            if (!proximoInicio.isAfter(ultimoInicio))
                agregarFranja(franjas, cancha, duracion, proximoInicio, ultimoInicio);
        }
        return List.copyOf(franjas);
    }
    private void agregarFranja(List<FranjaDisponible> franjas, Cancha cancha, int duracion,
                               LocalDateTime desde, LocalDateTime hasta) {
        if (!desde.isAfter(hasta)) franjas.add(new FranjaDisponible(cancha, duracion, desde, hasta));
    }
    public Reserva detalleReserva(long reservaId) {
        return repositorio.listar().stream().filter(r -> r.getId() == reservaId).findFirst()
            .orElseThrow(() -> new NegocioException("Reserva inexistente."));
    }
    private Cancha cancha(long id) {
        return canchas.stream().filter(c -> c.getId() == id).findFirst()
            .orElseThrow(() -> new NegocioException("Cancha inexistente."));
    }
    private void validarTurno(LocalDateTime inicio, int duracion) {
        if (inicio == null || !inicio.isAfter(LocalDateTime.now(reloj)))
            throw new NegocioException("El turno debe estar en el futuro.");
        if (inicio.toLocalTime().isBefore(LocalTime.of(9, 0))
                || inicio.toLocalTime().isAfter(LocalTime.of(22, 0)))
            throw new NegocioException("El horario de inicio debe estar entre las 09:00 y las 22:00.");
        if (duracion != 60 && duracion != 90) throw new NegocioException("La duración debe ser 60 o 90 minutos.");
        if (inicio.getSecond() != 0 || inicio.getNano() != 0) throw new NegocioException("Use precisión de minutos.");
        if (!inicio.toLocalDate().equals(inicio.plusMinutes(duracion).toLocalDate()))
            throw new NegocioException("En este incremento el turno debe finalizar el mismo día.");
    }
    public boolean disponible(long canchaId, LocalDateTime inicio, int duracion) {
        return disponibleExcepto(canchaId, inicio, duracion, -1L);
    }
    private boolean disponibleExcepto(long canchaId, LocalDateTime inicio, int duracion, long reservaIdExcluida) {
        validarTurno(inicio, duracion);
        cancha(canchaId).obtenerTarifa(tarifas, duracion);
        return reservas().stream().filter(r -> r.getId() != reservaIdExcluida)
            .noneMatch(r -> r.seSuperpone(canchaId, inicio, inicio.plusMinutes(duracion)));
    }
    public synchronized Reserva registrar(long clienteId, long canchaId, LocalDateTime inicio, int duracion) {
        Cliente cliente = clientes.stream().filter(c -> c.getId() == clienteId).findFirst()
            .orElseThrow(() -> new NegocioException("Cliente inexistente."));
        if (!disponible(canchaId, inicio, duracion)) throw new NegocioException("El horario está ocupado.");
        Cancha cancha = cancha(canchaId);
        Reserva reserva = new Reserva(repositorio.siguienteId(), 1L, cliente, cancha,
            cancha.obtenerTarifa(tarifas, duracion), inicio, LocalDateTime.now(reloj));
        repositorio.guardar(reserva); return reserva;
    }
    public synchronized Reserva modificar(long reservaId, long canchaId, LocalDateTime nuevoInicio, int duracion) {
        Reserva r = reserva(reservaId);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (r.getEstado() == EstadoReserva.CANCELADA)
            throw new NegocioException("No se puede modificar una reserva cancelada.");
        if (r.getInicio().minusHours(2).isBefore(ahora))
            throw new NegocioException("La modificación debe realizarse al menos dos horas antes del turno original.");
        Cancha nuevaCancha = cancha(canchaId);
        Tarifa nuevaTarifa = nuevaCancha.obtenerTarifa(tarifas, duracion);
        validarTurno(nuevoInicio, duracion);
        if (!disponibleExcepto(canchaId, nuevoInicio, duracion, reservaId))
            throw new NegocioException("El horario solicitado está ocupado.");
        if (r.getInicio().equals(nuevoInicio) && r.getCancha().getId() == canchaId
                && r.getDuracion() == duracion)
            throw new NegocioException("La modificación no introduce cambios.");
        r.modificar(nuevaCancha, nuevaTarifa, nuevoInicio, ahora);
        return r;
    }
    private Reserva reserva(long id) {
        return reservas().stream().filter(r -> r.getId() == id).findFirst()
            .orElseThrow(() -> new NegocioException("Reserva inexistente."));
    }
    public synchronized Pago registrarSena(long reservaId) {
        Reserva r = reserva(reservaId);
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (r.getEstado() == EstadoReserva.PENDIENTE && !ahora.isBefore(r.getVencimiento()))
            throw new NegocioException("Venció el plazo de 30 minutos para pagar la seña.");
        if (r.getEstado() != EstadoReserva.PENDIENTE && r.getEstado() != EstadoReserva.CONFIRMADA)
            throw new NegocioException("La reserva no admite pagos de seña.");
        BigDecimal diferencia = diferenciaSenaPendiente(r);
        if (diferencia.signum() <= 0) throw new NegocioException("La seña ya fue cubierta.");
        Pago pago = nuevoPago(r, TipoPago.SENA, diferencia, ahora);
        pagos.guardar(pago);
        if (r.getEstado() == EstadoReserva.PENDIENTE) r.confirmar();
        return pago;
    }
    public synchronized Pago registrarSaldo(long reservaId) {
        Reserva r = reserva(reservaId);
        if (r.getEstado() != EstadoReserva.CONFIRMADA) throw new NegocioException("Solo se cobra saldo de una reserva confirmada.");
        if (LocalDateTime.now(reloj).isBefore(r.getFin()))
            throw new NegocioException("El saldo se registra al finalizar el turno.");
        if (diferenciaSenaPendiente(r).signum() > 0)
            throw new NegocioException("Primero debe completarse el importe de la seña.");
        if (pagos.listarPorReserva(reservaId).stream().noneMatch(p -> p.getTipo() == TipoPago.SENA))
            throw new NegocioException("Primero debe registrarse la seña.");
        if (pagos.listarPorReserva(reservaId).stream().anyMatch(p -> p.getTipo() == TipoPago.SALDO))
            throw new NegocioException("El saldo ya fue registrado.");
        Pago pago = nuevoPago(r, TipoPago.SALDO, saldoPendiente(reservaId), LocalDateTime.now(reloj));
        pagos.guardar(pago);
        return pago;
    }
    public synchronized Reserva cancelar(long reservaId) {
        Reserva r = reserva(reservaId);
        List<Pago> movimientos = pagos.listarPorReserva(reservaId);
        BigDecimal senaPagada = movimientos.stream().filter(p -> p.getTipo() == TipoPago.SENA)
            .map(Pago::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldoPagado = movimientos.stream().filter(p -> p.getTipo() == TipoPago.SALDO)
            .map(Pago::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal yaDevuelto = movimientos.stream().filter(p -> p.getTipo() == TipoPago.DEVOLUCION)
            .map(Pago::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);
        r.cancelar();
        // Con 2 h o más se devuelve todo lo abonado; después del límite se retiene
        // la seña y se devuelve cualquier saldo pagado.
        boolean anticipada = !r.getInicio().minusHours(2).isBefore(LocalDateTime.now(reloj));
        BigDecimal importeADevolver = anticipada ? senaPagada.add(saldoPagado).subtract(yaDevuelto)
            : saldoPagado.subtract(yaDevuelto);
        if (importeADevolver.signum() > 0) {
            pagos.guardar(nuevoPago(r, TipoPago.DEVOLUCION, importeADevolver, LocalDateTime.now(reloj)));
        }
        return r;
    }
    public List<Pago> pagos(long reservaId) {
        reserva(reservaId);
        return pagos.listarPorReserva(reservaId);
    }
    public BigDecimal saldoPendiente(long reservaId) {
        Reserva r = reserva(reservaId);
        List<Pago> movimientos = pagos.listarPorReserva(reservaId);
        BigDecimal senaAbonada = movimientos.stream().filter(p -> p.getTipo() == TipoPago.SENA)
            .map(Pago::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal saldoAbonado = movimientos.stream().filter(p -> p.getTipo() == TipoPago.SALDO)
            .map(Pago::getImporte).reduce(BigDecimal.ZERO, BigDecimal::add);
        // La diferencia de seña y el saldo se informan por separado, sin contar
        // dos veces la diferencia que todavía falta completar.
        BigDecimal senaConsiderada = senaAbonada.max(r.getSena());
        return r.getPrecioTotal().subtract(senaConsiderada).subtract(saldoAbonado).max(BigDecimal.ZERO);
    }
    public BigDecimal diferenciaSenaPendiente(long reservaId) {
        return diferenciaSenaPendiente(reserva(reservaId));
    }
    private BigDecimal diferenciaSenaPendiente(Reserva r) {
        BigDecimal senaAbonada = pagos.listarPorReserva(r.getId()).stream()
            .filter(p -> p.getTipo() == TipoPago.SENA).map(Pago::getImporte)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return r.getSena().subtract(senaAbonada).max(BigDecimal.ZERO);
    }
    public BigDecimal creditoFavor(long reservaId) {
        Reserva r = reserva(reservaId);
        BigDecimal senaAbonada = pagos.listarPorReserva(reservaId).stream()
            .filter(p -> p.getTipo() == TipoPago.SENA).map(Pago::getImporte)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return senaAbonada.subtract(r.getSena()).max(BigDecimal.ZERO);
    }
    private Pago nuevoPago(Reserva r, TipoPago tipo, BigDecimal importe, LocalDateTime fecha) {
        return new Pago(pagos.siguienteId(), r.getId(), r.getUsuarioId(), tipo, importe, fecha);
    }
}
