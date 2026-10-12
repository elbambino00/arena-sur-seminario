package ar.arenasur.servicio;

import ar.arenasur.modelo.Cancha;
import ar.arenasur.modelo.Reserva;
import java.util.List;

public record AgendaCancha(Cancha cancha, List<Reserva> ocupaciones,
                           List<FranjaDisponible> franjasDisponibles) {
    public AgendaCancha {
        ocupaciones = List.copyOf(ocupaciones);
        franjasDisponibles = List.copyOf(franjasDisponibles);
    }
}
