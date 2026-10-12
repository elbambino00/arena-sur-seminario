package ar.arenasur.modelo;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public final class Pago {
    private final long id, reservaId, usuarioId;
    private final TipoPago tipo;
    private final BigDecimal importe;
    private final LocalDateTime fechaHora;
    public Pago(long id, long reservaId, long usuarioId, TipoPago tipo, BigDecimal importe, LocalDateTime fechaHora) {
        if (id <= 0 || reservaId <= 0 || usuarioId <= 0 || importe == null || importe.signum() <= 0)
            throw new IllegalArgumentException("Datos de pago inválidos.");
        this.id = id; this.reservaId = reservaId; this.usuarioId = usuarioId;
        this.tipo = Objects.requireNonNull(tipo); this.importe = importe;
        this.fechaHora = Objects.requireNonNull(fechaHora);
    }
    public long getId() { return id; }
    public long getReservaId() { return reservaId; }
    public long getUsuarioId() { return usuarioId; }
    public TipoPago getTipo() { return tipo; }
    public BigDecimal getImporte() { return importe; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    @Override public String toString() {
        return "Pago " + id + " | Reserva " + reservaId + " | " + tipo + " | $" + importe + " | " + fechaHora;
    }
}
