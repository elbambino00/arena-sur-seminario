package ar.arenasur.servicio;

import ar.arenasur.modelo.Cancha;
import java.time.LocalDateTime;

/** Intervalo inclusivo de horarios de inicio posibles para una duración. */
public record FranjaDisponible(Cancha cancha, int duracion, LocalDateTime desde, LocalDateTime hasta) { }
