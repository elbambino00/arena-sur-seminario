package ar.arenasur.repositorio;
import ar.arenasur.modelo.Reserva;
import java.util.List;
public interface ReservaRepositorio {
    long siguienteId();
    void guardar(Reserva reserva);
    List<Reserva> listar();
}
