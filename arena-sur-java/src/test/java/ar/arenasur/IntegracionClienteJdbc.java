package ar.arenasur;

import ar.arenasur.config.ConfiguracionBaseDatos;
import ar.arenasur.config.ConexionMySql;
import ar.arenasur.modelo.Cliente;
import ar.arenasur.modelo.DatosCliente;
import ar.arenasur.repositorio.ClienteRepositorio;
import ar.arenasur.repositorio.RegistroDuplicadoException;
import ar.arenasur.repositorio.jdbc.JdbcClienteRepositorio;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Prueba de integración de altas y consultas de clientes. */
public final class IntegracionClienteJdbc {
    private static final String EMAIL_PRUEBA = "integracion.cliente@example.com";
    private static int verificaciones;

    private IntegracionClienteJdbc() {
    }

    private static void comprobar(boolean condicion, String caso) {
        if (!condicion) {
            throw new AssertionError(caso);
        }
        verificaciones++;
        System.out.println("OK CLIENTE JDBC: " + caso);
    }

    private static void rechazaDuplicado(Runnable accion, String caso) {
        try {
            accion.run();
        } catch (RegistroDuplicadoException e) {
            comprobar(e.getMessage().equals("El email ya está registrado."), caso);
            return;
        }
        throw new AssertionError(caso);
    }

    public static void main(String[] args) throws SQLException {
        ConexionMySql conexiones = new ConexionMySql(ConfiguracionBaseDatos.desdeEntorno());
        ClienteRepositorio clientes = new JdbcClienteRepositorio(conexiones);
        limpiarClientePrueba(conexiones);

        try {
            comprobar(clientes.listarActivos().size() == 3,
                "lista los tres clientes iniciales");

            Cliente creado = clientes.guardar(new DatosCliente(
                "Carla", "Integración", "11-9999-0000", EMAIL_PRUEBA));
            comprobar(creado.getId() > 3 && creado.getNombreCompleto().equals("Carla Integración"),
                "inserta y devuelve el ID generado");
            comprobar(clientes.buscarActivo(creado.getId()).map(Cliente::getEmail)
                    .orElseThrow().equals(EMAIL_PRUEBA),
                "busca el cliente activo por ID");
            comprobar(clientes.buscarActivoPorEmail(EMAIL_PRUEBA.toUpperCase()).map(Cliente::getId)
                    .orElseThrow() == creado.getId(),
                "busca el email sin distinguir mayúsculas");
            comprobar(clientes.listarActivos().size() == 4,
                "incluye el alta en el listado activo");
            comprobar(clientes.buscarActivo(99999).isEmpty(),
                "informa cliente inexistente");

            rechazaDuplicado(() -> clientes.guardar(new DatosCliente(
                    "Duplicado", "Prueba", "11", "JUAN.PEREZ@EXAMPLE.COM")),
                "traduce la restricción de email duplicado");

            desactivarClientePrueba(conexiones, creado.getId());
            comprobar(clientes.buscarActivo(creado.getId()).isEmpty()
                    && clientes.listarActivos().size() == 3,
                "excluye clientes inactivos");
        } finally {
            limpiarClientePrueba(conexiones);
        }

        System.out.println("Resultado cliente JDBC: " + verificaciones
            + " verificaciones aprobadas.");
    }

    private static void desactivarClientePrueba(ConexionMySql conexiones, long id) throws SQLException {
        try (Connection conexion = conexiones.abrir();
             PreparedStatement actualizacion = conexion.prepareStatement(
                 "UPDATE cliente SET activo = FALSE WHERE id_cliente = ?")) {
            actualizacion.setLong(1, id);
            actualizacion.executeUpdate();
        }
    }

    private static void limpiarClientePrueba(ConexionMySql conexiones) throws SQLException {
        try (Connection conexion = conexiones.abrir();
             PreparedStatement borrado = conexion.prepareStatement(
                 "DELETE FROM cliente WHERE email = ?")) {
            borrado.setString(1, EMAIL_PRUEBA);
            borrado.executeUpdate();
        }
    }
}
