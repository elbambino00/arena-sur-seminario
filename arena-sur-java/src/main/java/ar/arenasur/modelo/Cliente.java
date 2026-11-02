package ar.arenasur.modelo;

public final class Cliente {
    private final long id;
    private final String nombre, apellido, telefono, email;
    public Cliente(long id, String nombre, String apellido, String telefono, String email) {
        if (id <= 0) throw new IllegalArgumentException("ID inválido.");
        DatosCliente datos = new DatosCliente(nombre, apellido, telefono, email);
        this.id = id;
        this.nombre = datos.nombre();
        this.apellido = datos.apellido();
        this.telefono = datos.telefono();
        this.email = datos.email();
    }
    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getEmail() { return email; }
    public String getNombreCompleto() { return nombre + " " + apellido; }
    public String getTelefono() { return telefono; }
    public String toString() { return id + " - " + getNombreCompleto(); }
}
