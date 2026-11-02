package ar.arenasur.repositorio;

/** Violación de una restricción única con un mensaje apto para la aplicación. */
public final class RegistroDuplicadoException extends PersistenciaException {
    public RegistroDuplicadoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
