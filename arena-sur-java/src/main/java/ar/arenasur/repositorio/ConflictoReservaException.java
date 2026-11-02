package ar.arenasur.repositorio;

/** Operación válida en forma pero incompatible con el estado actual de la reserva o agenda. */
public final class ConflictoReservaException extends PersistenciaException {
    public ConflictoReservaException(String mensaje) {
        super(mensaje);
    }

    public ConflictoReservaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
