package ar.arenasur.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Diagnóstico manual de conectividad. No imprime credenciales. */
public final class VerificarConexion {
    private VerificarConexion() {
    }

    public static void main(String[] args) {
        try {
            ConfiguracionBaseDatos configuracion = ConfiguracionBaseDatos.desdeEntorno();
            ConexionMySql fabrica = new ConexionMySql(configuracion);

            try (Connection conexion = fabrica.abrir();
                 Statement consulta = conexion.createStatement();
                 ResultSet resultado = consulta.executeQuery("SELECT DATABASE()")) {
                resultado.next();
                DatabaseMetaData metadata = conexion.getMetaData();
                System.out.println("Conexión MySQL correcta");
                System.out.println("Servidor: " + metadata.getDatabaseProductVersion());
                System.out.println("Base: " + resultado.getString(1));
            }
        } catch (IllegalArgumentException e) {
            System.err.println("Configuración incompleta: " + e.getMessage());
            System.exit(2);
        } catch (SQLException e) {
            System.err.println("No se pudo conectar a MySQL: " + e.getMessage());
            System.exit(1);
        }
    }
}
