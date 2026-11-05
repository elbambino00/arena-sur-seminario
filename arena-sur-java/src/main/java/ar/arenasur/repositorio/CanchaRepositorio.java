package ar.arenasur.repositorio;

import ar.arenasur.modelo.Cancha;
import java.util.List;
import java.util.Optional;

public interface CanchaRepositorio {
    Cancha guardar(String nombre, String deporte);
    Cancha modificarNombre(long id, String nombre);
    void desactivar(long id);
    List<Cancha> listarActivas();
    Optional<Cancha> buscarActiva(long id);
}
