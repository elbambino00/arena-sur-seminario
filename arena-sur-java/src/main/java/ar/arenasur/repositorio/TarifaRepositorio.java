package ar.arenasur.repositorio;

import ar.arenasur.modelo.Tarifa;
import java.util.List;
import java.util.Optional;

public interface TarifaRepositorio {
    List<Tarifa> listarActivas();
    Optional<Tarifa> buscarActiva(String deporte, int duracion);
}
