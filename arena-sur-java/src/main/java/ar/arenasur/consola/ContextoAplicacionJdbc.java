package ar.arenasur.consola;

import ar.arenasur.config.ConfiguracionBaseDatos;
import ar.arenasur.config.ConexionMySql;
import ar.arenasur.repositorio.CanchaRepositorio;
import ar.arenasur.repositorio.ClienteRepositorio;
import ar.arenasur.repositorio.DeporteRepositorio;
import ar.arenasur.repositorio.ReservaTransaccionalRepositorio;
import ar.arenasur.repositorio.TarifaRepositorio;
import ar.arenasur.repositorio.jdbc.JdbcCanchaRepositorio;
import ar.arenasur.repositorio.jdbc.JdbcClienteRepositorio;
import ar.arenasur.repositorio.jdbc.JdbcDeporteRepositorio;
import ar.arenasur.repositorio.jdbc.JdbcReservaTransaccionalRepositorio;
import ar.arenasur.repositorio.jdbc.JdbcTarifaRepositorio;

/** Dependencias persistentes que utilizará la consola durante la migración a JDBC. */
public final class ContextoAplicacionJdbc {
    private final ConexionMySql conexiones;
    private final DeporteRepositorio deportes;
    private final CanchaRepositorio canchas;
    private final TarifaRepositorio tarifas;
    private final ClienteRepositorio clientes;
    private final ReservaTransaccionalRepositorio reservas;

    private ContextoAplicacionJdbc(ConexionMySql conexiones) {
        this.conexiones = conexiones;
        this.deportes = new JdbcDeporteRepositorio(conexiones);
        this.canchas = new JdbcCanchaRepositorio(conexiones);
        this.tarifas = new JdbcTarifaRepositorio(conexiones);
        this.clientes = new JdbcClienteRepositorio(conexiones);
        this.reservas = new JdbcReservaTransaccionalRepositorio(conexiones);
    }

    public static ContextoAplicacionJdbc desdeEntorno() {
        return new ContextoAplicacionJdbc(
            new ConexionMySql(ConfiguracionBaseDatos.desdeEntorno()));
    }

    public ConexionMySql conexiones() { return conexiones; }
    public DeporteRepositorio deportes() { return deportes; }
    public CanchaRepositorio canchas() { return canchas; }
    public TarifaRepositorio tarifas() { return tarifas; }
    public ClienteRepositorio clientes() { return clientes; }
    public ReservaTransaccionalRepositorio reservas() { return reservas; }
}
