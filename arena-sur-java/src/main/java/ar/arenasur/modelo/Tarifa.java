package ar.arenasur.modelo;
import java.math.BigDecimal;

public record Tarifa(long id, String deporte, int duracion, BigDecimal precio) {
    public Tarifa {
        if (id <= 0 || deporte == null || deporte.isBlank() || (duracion != 60 && duracion != 90)
                || precio == null || precio.signum() <= 0)
            throw new IllegalArgumentException("Tarifa inválida.");
    }
}
