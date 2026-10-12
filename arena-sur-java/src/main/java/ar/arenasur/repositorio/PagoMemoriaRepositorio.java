package ar.arenasur.repositorio;
import ar.arenasur.modelo.Pago;
import java.util.*;
public final class PagoMemoriaRepositorio implements PagoRepositorio {
    private final List<Pago> pagos = new ArrayList<>();
    private long secuencia = 1;
    public long siguienteId() { return secuencia++; }
    public void guardar(Pago pago) { pagos.add(Objects.requireNonNull(pago)); }
    public List<Pago> listarPorReserva(long reservaId) {
        return pagos.stream().filter(p -> p.getReservaId() == reservaId).toList();
    }
}
