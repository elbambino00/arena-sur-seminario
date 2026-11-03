package ar.arenasur.repositorio;

import ar.arenasur.modelo.Pago;
import ar.arenasur.modelo.Reserva;
import ar.arenasur.modelo.ModificacionReserva;
import ar.arenasur.modelo.SolicitudReserva;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Persistencia del agregado Reserva y sus pagos con límites transaccionales explícitos. */
public interface ReservaTransaccionalRepositorio {
    Reserva crearPendiente(SolicitudReserva solicitud);
    Optional<Reserva> buscar(long id);
    List<Reserva> listar();
    Pago registrarSena(long reservaId, long usuarioId, LocalDateTime fechaHora);
    Reserva modificar(long reservaId, ModificacionReserva modificacion);
    Pago registrarSaldo(long reservaId, long usuarioId, LocalDateTime fechaHora);
    Reserva cancelar(long reservaId, long usuarioId, LocalDateTime fechaHora);
    List<Pago> listarPagos(long reservaId);
}
