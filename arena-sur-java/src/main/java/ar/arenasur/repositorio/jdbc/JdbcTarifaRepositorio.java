package ar.arenasur.repositorio.jdbc;

import ar.arenasur.config.ConexionMySql;
import ar.arenasur.modelo.Tarifa;
import ar.arenasur.repositorio.PersistenciaException;
import ar.arenasur.repositorio.TarifaRepositorio;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcTarifaRepositorio implements TarifaRepositorio {
    private static final String COLUMNAS = """
        SELECT t.id_tarifa, d.nombre AS deporte, t.duracion_minutos, t.precio
        FROM tarifa t
        JOIN deporte d ON d.id_deporte = t.id_deporte
        """;
    private static final String LISTAR = COLUMNAS + """
        WHERE t.activa = TRUE AND d.activo = TRUE
        ORDER BY t.id_tarifa
        """;
    private static final String BUSCAR = COLUMNAS + """
        WHERE LOWER(d.nombre) = LOWER(?)
          AND t.duracion_minutos = ?
          AND t.activa = TRUE
          AND d.activo = TRUE
        """;

    private final ConexionMySql conexiones;

    public JdbcTarifaRepositorio(ConexionMySql conexiones) {
        this.conexiones = conexiones;
    }

    @Override
    public List<Tarifa> listarActivas() {
        List<Tarifa> tarifas = new ArrayList<>();
        try (Connection conexion = conexiones.abrir();
             PreparedStatement consulta = conexion.prepareStatement(LISTAR);
             ResultSet resultados = consulta.executeQuery()) {
            while (resultados.next()) {
                tarifas.add(mapear(resultados));
            }
            return List.copyOf(tarifas);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar las tarifas.", e);
        }
    }

    @Override
    public Optional<Tarifa> buscarActiva(String deporte, int duracion) {
        if (deporte == null || deporte.isBlank()) {
            throw new IllegalArgumentException("El deporte es obligatorio.");
        }
        try (Connection conexion = conexiones.abrir();
             PreparedStatement consulta = conexion.prepareStatement(BUSCAR)) {
            consulta.setString(1, deporte.trim());
            consulta.setInt(2, duracion);
            try (ResultSet resultados = consulta.executeQuery()) {
                return resultados.next() ? Optional.of(mapear(resultados)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar la tarifa.", e);
        }
    }

    private Tarifa mapear(ResultSet resultados) throws SQLException {
        return new Tarifa(
            resultados.getLong("id_tarifa"),
            resultados.getString("deporte"),
            resultados.getInt("duracion_minutos"),
            resultados.getBigDecimal("precio"));
    }
}
