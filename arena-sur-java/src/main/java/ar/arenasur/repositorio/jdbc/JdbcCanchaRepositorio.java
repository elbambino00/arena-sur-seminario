package ar.arenasur.repositorio.jdbc;

import ar.arenasur.config.ConexionMySql;
import ar.arenasur.modelo.Cancha;
import ar.arenasur.modelo.CanchaFutbol;
import ar.arenasur.modelo.CanchaPadel;
import ar.arenasur.modelo.CanchaTenis;
import ar.arenasur.repositorio.CanchaRepositorio;
import ar.arenasur.repositorio.PersistenciaException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcCanchaRepositorio implements CanchaRepositorio {
    private static final String COLUMNAS = """
        SELECT c.id_cancha, c.nombre, d.nombre AS deporte
        FROM cancha c
        JOIN deporte d ON d.id_deporte = c.id_deporte
        """;
    private static final String LISTAR = COLUMNAS + """
        WHERE c.activa = TRUE AND d.activo = TRUE
        ORDER BY c.id_cancha
        """;
    private static final String BUSCAR = COLUMNAS + """
        WHERE c.id_cancha = ? AND c.activa = TRUE AND d.activo = TRUE
        """;

    private final ConexionMySql conexiones;

    public JdbcCanchaRepositorio(ConexionMySql conexiones) {
        this.conexiones = conexiones;
    }

    @Override
    public List<Cancha> listarActivas() {
        List<Cancha> canchas = new ArrayList<>();
        try (Connection conexion = conexiones.abrir();
             PreparedStatement consulta = conexion.prepareStatement(LISTAR);
             ResultSet resultados = consulta.executeQuery()) {
            while (resultados.next()) {
                canchas.add(mapear(resultados));
            }
            return List.copyOf(canchas);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar las canchas.", e);
        }
    }

    @Override
    public Optional<Cancha> buscarActiva(long id) {
        try (Connection conexion = conexiones.abrir();
             PreparedStatement consulta = conexion.prepareStatement(BUSCAR)) {
            consulta.setLong(1, id);
            try (ResultSet resultados = consulta.executeQuery()) {
                return resultados.next() ? Optional.of(mapear(resultados)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar la cancha.", e);
        }
    }

    private Cancha mapear(ResultSet resultados) throws SQLException {
        long id = resultados.getLong("id_cancha");
        String nombre = resultados.getString("nombre");
        String deporte = resultados.getString("deporte");
        return switch (deporte) {
            case "F\u00FAtbol 5" -> new CanchaFutbol(id, nombre);
            case "P\u00E1del" -> new CanchaPadel(id, nombre);
            case "Tenis" -> new CanchaTenis(id, nombre);
            default -> throw new PersistenciaException("Deporte de cancha no soportado: " + deporte);
        };
    }
}
