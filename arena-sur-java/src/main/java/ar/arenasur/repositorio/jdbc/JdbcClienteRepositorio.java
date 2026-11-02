package ar.arenasur.repositorio.jdbc;

import ar.arenasur.config.ConexionMySql;
import ar.arenasur.modelo.Cliente;
import ar.arenasur.modelo.DatosCliente;
import ar.arenasur.repositorio.ClienteRepositorio;
import ar.arenasur.repositorio.PersistenciaException;
import ar.arenasur.repositorio.RegistroDuplicadoException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcClienteRepositorio implements ClienteRepositorio {
    private static final int MYSQL_ENTRADA_DUPLICADA = 1062;
    private static final String INSERTAR = """
        INSERT INTO cliente (nombre, apellido, telefono, email, activo)
        VALUES (?, ?, ?, ?, TRUE)
        """;
    private static final String LISTAR = """
        SELECT id_cliente, nombre, apellido, telefono, email
        FROM cliente
        WHERE activo = TRUE
        ORDER BY id_cliente
        """;
    private static final String BUSCAR_ID = """
        SELECT id_cliente, nombre, apellido, telefono, email
        FROM cliente
        WHERE id_cliente = ? AND activo = TRUE
        """;
    private static final String BUSCAR_EMAIL = """
        SELECT id_cliente, nombre, apellido, telefono, email
        FROM cliente
        WHERE email = ? AND activo = TRUE
        """;

    private final ConexionMySql conexiones;

    public JdbcClienteRepositorio(ConexionMySql conexiones) {
        this.conexiones = conexiones;
    }

    @Override
    public Cliente guardar(DatosCliente datos) {
        try (Connection conexion = conexiones.abrir();
             PreparedStatement insercion = conexion.prepareStatement(
                 INSERTAR, Statement.RETURN_GENERATED_KEYS)) {
            insercion.setString(1, datos.nombre());
            insercion.setString(2, datos.apellido());
            insercion.setString(3, datos.telefono());
            insercion.setString(4, datos.email());
            insercion.executeUpdate();

            try (ResultSet claves = insercion.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new PersistenciaException("MySQL no devolvió el ID del cliente registrado.");
                }
                return new Cliente(
                    claves.getLong(1), datos.nombre(), datos.apellido(), datos.telefono(), datos.email());
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == MYSQL_ENTRADA_DUPLICADA) {
                throw new RegistroDuplicadoException("El email ya está registrado.", e);
            }
            throw new PersistenciaException("No se pudo registrar el cliente.", e);
        }
    }

    @Override
    public List<Cliente> listarActivos() {
        List<Cliente> clientes = new ArrayList<>();
        try (Connection conexion = conexiones.abrir();
             PreparedStatement consulta = conexion.prepareStatement(LISTAR);
             ResultSet resultados = consulta.executeQuery()) {
            while (resultados.next()) {
                clientes.add(mapear(resultados));
            }
            return List.copyOf(clientes);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los clientes.", e);
        }
    }

    @Override
    public Optional<Cliente> buscarActivo(long id) {
        try (Connection conexion = conexiones.abrir();
             PreparedStatement consulta = conexion.prepareStatement(BUSCAR_ID)) {
            consulta.setLong(1, id);
            return primerResultado(consulta);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el cliente.", e);
        }
    }

    @Override
    public Optional<Cliente> buscarActivoPorEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("El email es obligatorio.");
        }
        try (Connection conexion = conexiones.abrir();
             PreparedStatement consulta = conexion.prepareStatement(BUSCAR_EMAIL)) {
            consulta.setString(1, email.trim());
            return primerResultado(consulta);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar el cliente por email.", e);
        }
    }

    private Optional<Cliente> primerResultado(PreparedStatement consulta) throws SQLException {
        try (ResultSet resultados = consulta.executeQuery()) {
            return resultados.next() ? Optional.of(mapear(resultados)) : Optional.empty();
        }
    }

    private Cliente mapear(ResultSet resultados) throws SQLException {
        return new Cliente(
            resultados.getLong("id_cliente"),
            resultados.getString("nombre"),
            resultados.getString("apellido"),
            resultados.getString("telefono"),
            resultados.getString("email"));
    }
}
