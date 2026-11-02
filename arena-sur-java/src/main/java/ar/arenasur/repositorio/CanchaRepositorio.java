package ar.arenasur.repositorio;

import ar.arenasur.modelo.Cancha;
import java.util.List;
import java.util.Optional;

public interface CanchaRepositorio {
    List<Cancha> listarActivas();
    Optional<Cancha> buscarActiva(long id);
}
