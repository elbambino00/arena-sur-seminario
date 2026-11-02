package ar.arenasur.modelo;

import java.time.LocalDateTime;

/** Datos necesarios para registrar una reserva pendiente. */
public record SolicitudReserva(long clienteId, long canchaId, int duracion,
                               LocalDateTime inicio, long usuarioId,
                               LocalDateTime fechaHoraCreacion) {
    public SolicitudReserva {
        if (clienteId <= 0 || canchaId <= 0 || usuarioId <= 0) {
            throw new IllegalArgumentException("Identificadores de reserva inválidos.");
        }
        if (duracion != 60 && duracion != 90) {
            throw new IllegalArgumentException("La duración debe ser 60 o 90 minutos.");
        }
        if (inicio == null || fechaHoraCreacion == null || !inicio.isAfter(fechaHoraCreacion)) {
            throw new IllegalArgumentException("El turno debe estar en el futuro.");
        }
        if (inicio.getSecond() != 0 || inicio.getNano() != 0) {
            throw new IllegalArgumentException("Use precisión de minutos.");
        }
        if (inicio.toLocalTime().isBefore(java.time.LocalTime.of(9, 0))
                || inicio.toLocalTime().isAfter(java.time.LocalTime.of(22, 0))) {
            throw new IllegalArgumentException("El horario debe comenzar entre las 09:00 y las 22:00.");
        }
        if (!inicio.toLocalDate().equals(inicio.plusMinutes(duracion).toLocalDate())) {
            throw new IllegalArgumentException("El turno debe finalizar el mismo día.");
        }
    }
}
