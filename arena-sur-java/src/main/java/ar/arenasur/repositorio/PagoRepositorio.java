package ar.arenasur.repositorio;
import ar.arenasur.modelo.Pago;
import java.util.List;
public interface PagoRepositorio {
    long siguienteId();
    void guardar(Pago pago);
    List<Pago> listarPorReserva(long reservaId);
}
