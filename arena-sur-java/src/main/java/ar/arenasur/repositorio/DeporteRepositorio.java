package ar.arenasur.repositorio;

import ar.arenasur.modelo.Deporte;
import java.util.List;
import java.util.Optional;

public interface DeporteRepositorio {
    List<Deporte> listarActivos();
    Optional<Deporte> buscarActivo(long id);
}
