package ar.arenasur.servicio;

import java.time.LocalDate;
import java.util.List;

public record AgendaDiaria(LocalDate fecha, List<AgendaCancha> canchas) {
    public AgendaDiaria { canchas = List.copyOf(canchas); }
}
