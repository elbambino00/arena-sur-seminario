package ar.arenasur.modelo;

import java.time.LocalDateTime;

/** Datos del nuevo turno solicitado para una reserva existente. */
public record ModificacionReserva(long canchaId, int duracion, LocalDateTime inicio,
                                  LocalDateTime ahora) {
    public ModificacionReserva {
        if (canchaId <= 0 || (duracion != 60 && duracion != 90)
                || inicio == null || ahora == null || !inicio.isAfter(ahora)) {
            throw new IllegalArgumentException("Datos de modificación inválidos.");
        }
        if (inicio.getSecond() != 0 || inicio.getNano() != 0
                || inicio.toLocalTime().isBefore(java.time.LocalTime.of(9, 0))
                || inicio.toLocalTime().isAfter(java.time.LocalTime.of(22, 0))
                || !inicio.toLocalDate().equals(inicio.plusMinutes(duracion).toLocalDate())) {
            throw new IllegalArgumentException("El nuevo turno no tiene un horario válido.");
        }
    }
}
