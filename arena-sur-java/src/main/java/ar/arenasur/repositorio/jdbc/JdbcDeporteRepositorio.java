package ar.arenasur.repositorio.jdbc;

import ar.arenasur.config.ConexionMySql;
import ar.arenasur.modelo.Deporte;
import ar.arenasur.repositorio.DeporteRepositorio;
import ar.arenasur.repositorio.PersistenciaException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcDeporteRepositorio implements DeporteRepositorio {
    private static final String LISTAR = """
        SELECT id_deporte, nombre, activo
        FROM deporte
        WHERE activo = TRUE
        ORDER BY id_deporte
        """;
    private static final String BUSCAR = """
        SELECT id_deporte, nombre, activo
        FROM deporte
        WHERE id_deporte = ? AND activo = TRUE
        """;

    private final ConexionMySql conexiones;

    public JdbcDeporteRepositorio(ConexionMySql conexiones) {
        this.conexiones = conexiones;
    }

    @Override
    public List<Deporte> listarActivos() {
        List<Deporte> deportes = new ArrayList<>();
        try (Connection conexion = conexiones.abrir();
             PreparedStatement consulta = conexion.prepareStatement(LISTAR);
             ResultSet resultados = consulta.executeQuery()) {
            while (resultados.next()) {
                deportes.add(mapear(resultados));
            }
            return List.copyOf(deportes);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los deportes.", e);
        }
    }

    @Override
    public Optional<Deporte> buscarActivo(long id) {
        try (Connection conexion = conexiones.abrir();
             PreparedStatement consulta = conexion.prepareStatement(BUSCAR)) {
            consulta.setLong(1, id);
            try (ResultSet resultados = consulta.executeQuery()) {
                return resultados.next() ? Optional.of(mapear(resultados)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el deporte.", e);
        }
    }

    private Deporte mapear(ResultSet resultados) throws SQLException {
        return new Deporte(
            resultados.getLong("id_deporte"),
            resultados.getString("nombre"),
            resultados.getBoolean("activo"));
    }
}
