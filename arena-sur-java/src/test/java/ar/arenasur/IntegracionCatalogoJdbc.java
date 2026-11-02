package ar.arenasur;

import ar.arenasur.config.ConfiguracionBaseDatos;
import ar.arenasur.config.ConexionMySql;
import ar.arenasur.modelo.Cancha;
import ar.arenasur.modelo.CanchaFutbol;
import ar.arenasur.modelo.CanchaPadel;
import ar.arenasur.modelo.CanchaTenis;
import ar.arenasur.modelo.Deporte;
import ar.arenasur.modelo.Tarifa;
import ar.arenasur.repositorio.CanchaRepositorio;
import ar.arenasur.repositorio.DeporteRepositorio;
import ar.arenasur.repositorio.ReservaMemoriaRepositorio;
import ar.arenasur.repositorio.TarifaRepositorio;
import ar.arenasur.repositorio.jdbc.JdbcCanchaRepositorio;
import ar.arenasur.repositorio.jdbc.JdbcDeporteRepositorio;
import ar.arenasur.repositorio.jdbc.JdbcTarifaRepositorio;
import ar.arenasur.servicio.ReservaService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/** Prueba de integración ejecutable contra el esquema MySQL de desarrollo. */
public final class IntegracionCatalogoJdbc {
    private static int verificaciones;

    private IntegracionCatalogoJdbc() {
    }

    private static void comprobar(boolean condicion, String caso) {
        if (!condicion) {
            throw new AssertionError(caso);
        }
        verificaciones++;
        System.out.println("OK JDBC: " + caso);
    }

    public static void main(String[] args) {
        ConexionMySql conexiones = new ConexionMySql(ConfiguracionBaseDatos.desdeEntorno());
        DeporteRepositorio deportes = new JdbcDeporteRepositorio(conexiones);
        CanchaRepositorio canchas = new JdbcCanchaRepositorio(conexiones);
        TarifaRepositorio tarifas = new JdbcTarifaRepositorio(conexiones);

        List<Deporte> deportesActivos = deportes.listarActivos();
        comprobar(deportesActivos.size() == 3, "lista los tres deportes activos");
        comprobar(deportes.buscarActivo(2).map(Deporte::nombre).orElseThrow().equals("Pádel"),
            "busca un deporte activo por ID");
        comprobar(deportes.buscarActivo(999).isEmpty(), "informa deporte inexistente");

        List<Cancha> canchasActivas = canchas.listarActivas();
        comprobar(canchasActivas.size() == 5, "lista las cinco canchas activas");
        comprobar(canchasActivas.stream().filter(CanchaFutbol.class::isInstance).count() == 2
                && canchasActivas.stream().filter(CanchaPadel.class::isInstance).count() == 2
                && canchasActivas.stream().filter(CanchaTenis.class::isInstance).count() == 1,
            "mapea cada deporte al subtipo de cancha correcto");
        comprobar(canchas.buscarActiva(5).map(Cancha::getNombre).orElseThrow().equals("Tenis 1"),
            "busca una cancha activa por ID");
        comprobar(canchas.buscarActiva(999).isEmpty(), "informa cancha inexistente");

        List<Tarifa> tarifasActivas = tarifas.listarActivas();
        comprobar(tarifasActivas.size() == 6, "lista las seis tarifas activas");
        comprobar(tarifas.buscarActiva("pÁdel", 90).map(Tarifa::precio).orElseThrow()
                .compareTo(new BigDecimal("80000.00")) == 0,
            "busca tarifa sin distinguir mayúsculas ni acentos de capitalización");
        comprobar(tarifas.buscarActiva("Tenis", 45).isEmpty(), "informa tarifa inexistente");

        Clock reloj = Clock.fixed(
            Instant.parse("2026-10-12T15:00:00Z"),
            ZoneId.of("America/Argentina/Buenos_Aires"));
        ReservaService servicio = new ReservaService(
            new ReservaMemoriaRepositorio(), reloj, canchasActivas, tarifasActivas);
        comprobar(servicio.canchas().size() == 5 && servicio.tarifas().size() == 6,
            "el catálogo JDBC es compatible con ReservaService");

        System.out.println("Resultado JDBC: " + verificaciones + " verificaciones aprobadas.");
    }
}
