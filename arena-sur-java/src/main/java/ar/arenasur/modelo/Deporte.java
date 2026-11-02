package ar.arenasur.modelo;

/** Deporte configurable recuperado del catálogo persistente. */
public record Deporte(long id, String nombre, boolean activo) {
    public Deporte {
        if (id <= 0 || nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Deporte inválido.");
        }
        nombre = nombre.trim();
    }
}
