package ar.arenasur.modelo;

public final class Cliente {
    private final long id;
    private final String nombre, apellido, telefono, email;
    public Cliente(long id, String nombre, String apellido, String telefono, String email) {
        if (id <= 0) throw new IllegalArgumentException("ID inválido.");
        this.id = id;
        this.nombre = validar(nombre); this.apellido = validar(apellido);
        this.telefono = validar(telefono); this.email = validar(email);
        if (!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("Email inválido.");
    }
    private static String validar(String valor) {
        if (valor == null || valor.isBlank()) throw new IllegalArgumentException("Complete todos los datos.");
        return valor.trim();
    }
    public long getId() { return id; }
    public String getEmail() { return email; }
    public String getNombreCompleto() { return nombre + " " + apellido; }
    public String getTelefono() { return telefono; }
    public String toString() { return id + " - " + getNombreCompleto(); }
}
