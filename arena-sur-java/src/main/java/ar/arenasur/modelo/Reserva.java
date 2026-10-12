package ar.arenasur.modelo;
import java.math.*;
import java.time.*;
import java.util.Objects;

public final class Reserva {
    private final long id, usuarioId;
    private final Cliente cliente;
    private Cancha cancha;
    private Tarifa tarifa;
    private LocalDateTime inicio;
    private final LocalDateTime creacion;
    private LocalDateTime vencimiento;
    private EstadoReserva estado = EstadoReserva.PENDIENTE;
    public Reserva(long id, long usuarioId, Cliente cliente, Cancha cancha, Tarifa tarifa,
                   LocalDateTime inicio, LocalDateTime creacion) {
        if (id <= 0 || usuarioId <= 0) throw new IllegalArgumentException("Identificador inválido.");
        this.id = id; this.usuarioId = usuarioId;
        this.cliente = Objects.requireNonNull(cliente); this.cancha = Objects.requireNonNull(cancha);
        this.tarifa = Objects.requireNonNull(tarifa);
        this.inicio = Objects.requireNonNull(inicio); this.creacion = Objects.requireNonNull(creacion);
        if (!tarifa.deporte().equals(cancha.getDeporte()) || !inicio.isAfter(creacion))
            throw new IllegalArgumentException("Fecha o tarifa incompatible.");
        vencimiento = creacion.plusMinutes(30);
    }
    public void actualizarVencimiento(LocalDateTime ahora) {
        if (estado == EstadoReserva.PENDIENTE && !ahora.isBefore(vencimiento))
            estado = EstadoReserva.CANCELADA;
    }
    public boolean seSuperpone(long canchaId, LocalDateTime desde, LocalDateTime hasta) {
        return estado != EstadoReserva.CANCELADA && cancha.getId() == canchaId
                && desde.isBefore(getFin()) && hasta.isAfter(inicio);
    }
    public long getId() { return id; }
    public long getUsuarioId() { return usuarioId; }
    public Cliente getCliente() { return cliente; }
    public Cancha getCancha() { return cancha; }
    public Tarifa getTarifa() { return tarifa; }
    public LocalDateTime getInicio() { return inicio; }
    public LocalDateTime getFin() { return inicio.plusMinutes(tarifa.duracion()); }
    public int getDuracion() { return tarifa.duracion(); }
    public LocalDateTime getVencimiento() { return vencimiento; }
    public EstadoReserva getEstado() { return estado; }
    public BigDecimal getPrecioTotal() { return tarifa.precio(); }
    public BigDecimal getSena() { return tarifa.precio().multiply(new BigDecimal("0.20")).setScale(2, RoundingMode.HALF_UP); }
    public BigDecimal getSaldo() { return tarifa.precio().subtract(getSena()); }
    public void confirmar() {
        if (estado != EstadoReserva.PENDIENTE) throw new IllegalStateException("La reserva no está pendiente.");
        estado = EstadoReserva.CONFIRMADA;
    }
    public void cancelar() {
        if (estado == EstadoReserva.CANCELADA) throw new IllegalStateException("La reserva ya está cancelada.");
        estado = EstadoReserva.CANCELADA;
    }
    public void modificar(Cancha nuevaCancha, Tarifa nuevaTarifa, LocalDateTime nuevoInicio,
                          LocalDateTime ahora) {
        if (estado == EstadoReserva.CANCELADA) throw new IllegalStateException("La reserva está cancelada.");
        if (nuevaCancha == null || nuevaTarifa == null || nuevoInicio == null || ahora == null)
            throw new IllegalArgumentException("Los datos de modificación son obligatorios.");
        if (!nuevaTarifa.deporte().equals(nuevaCancha.getDeporte()) || !nuevoInicio.isAfter(ahora))
            throw new IllegalArgumentException("Fecha o tarifa incompatible.");
        cancha = nuevaCancha;
        tarifa = nuevaTarifa;
        inicio = nuevoInicio;
        if (estado == EstadoReserva.PENDIENTE) vencimiento = ahora.plusMinutes(30);
    }
    public String toString() {
        return id + " | " + cliente.getNombreCompleto() + " | " + cancha.getNombre()
            + " | " + inicio + " a " + getFin() + " | " + estado
            + " | Total: $" + getPrecioTotal() + " | Seña: $" + getSena()
            + " | Vence: " + vencimiento;
    }
}
