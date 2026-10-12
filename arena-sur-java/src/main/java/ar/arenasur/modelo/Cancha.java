package ar.arenasur.modelo;
import java.util.List;

/** Cada deporte selecciona sus tarifas; los precios permanecen en Tarifa, como en el DER. */
public abstract class Cancha {
    private final long id;
    private final String nombre;
    protected Cancha(long id, String nombre) {
        if (id <= 0 || nombre == null || nombre.isBlank()) throw new IllegalArgumentException("Cancha inválida.");
        this.id = id; this.nombre = nombre;
    }
    public long getId() { return id; }
    public String getNombre() { return nombre; }
    public abstract String getDeporte();
    public Tarifa obtenerTarifa(List<Tarifa> tarifas, int duracion) {
        return tarifas.stream().filter(t -> t.deporte().equals(getDeporte()) && t.duracion() == duracion)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("No existe tarifa para ese turno."));
    }
    public String toString() { return id + " - " + nombre + " (" + getDeporte() + ")"; }
}
