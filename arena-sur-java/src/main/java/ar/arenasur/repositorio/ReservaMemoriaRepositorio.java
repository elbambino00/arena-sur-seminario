package ar.arenasur.repositorio;
import ar.arenasur.modelo.Reserva;
import java.util.*;
public final class ReservaMemoriaRepositorio implements ReservaRepositorio {
    private final List<Reserva> reservas = new ArrayList<>();
    private long secuencia = 1;
    public long siguienteId() { return secuencia++; }
    public void guardar(Reserva reserva) { reservas.add(Objects.requireNonNull(reserva)); }
    public List<Reserva> listar() { return List.copyOf(reservas); }
}
