package ar.arenasur.modelo;

/** Datos validados necesarios para registrar un cliente nuevo. */
public record DatosCliente(String nombre, String apellido, String telefono, String email) {
    public DatosCliente {
        nombre = validar(nombre);
        apellido = validar(apellido);
        telefono = validar(telefono);
        email = validar(email);
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new IllegalArgumentException("Email inválido.");
        }
    }

    private static String validar(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Complete todos los datos.");
        }
        return valor.trim();
    }
}
