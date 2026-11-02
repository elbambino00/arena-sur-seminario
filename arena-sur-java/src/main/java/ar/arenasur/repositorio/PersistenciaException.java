package ar.arenasur.repositorio;

/** Evita propagar detalles JDBC fuera de la capa de persistencia. */
public class PersistenciaException extends RuntimeException {
    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    public PersistenciaException(String mensaje) {
        super(mensaje);
    }
}
