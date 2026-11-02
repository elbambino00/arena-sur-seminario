package ar.arenasur;

import ar.arenasur.config.ConfiguracionBaseDatos;
import ar.arenasur.config.ConexionMySql;
import ar.arenasur.modelo.EstadoReserva;
import ar.arenasur.modelo.Pago;
import ar.arenasur.modelo.Reserva;
import ar.arenasur.modelo.SolicitudReserva;
import ar.arenasur.modelo.TipoPago;
import ar.arenasur.repositorio.ConflictoReservaException;
import ar.arenasur.repositorio.ReservaTransaccionalRepositorio;
import ar.arenasur.repositorio.jdbc.JdbcReservaTransaccionalRepositorio;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Prueba de integración transaccional de reservas y señas. */
public final class IntegracionReservaJdbc {
    private static final LocalDate FECHA_DESDE = LocalDate.of(2026, 12, 15);
    private static final LocalDate FECHA_HASTA = LocalDate.of(2026, 12, 17);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 2, 12, 0);
    private static int verificaciones;

    private IntegracionReservaJdbc() {
    }

    private static void comprobar(boolean condicion, String caso) {
        if (!condicion) {
            throw new AssertionError(caso);
        }
        verificaciones++;
        System.out.println("OK RESERVA JDBC: " + caso);
    }

    private static void rechazaConflicto(Runnable accion, String mensaje, String caso) {
        try {
            accion.run();
        } catch (ConflictoReservaException e) {
            comprobar(e.getMessage().equals(mensaje), caso);
            return;
        }
        throw new AssertionError(caso);
    }

    public static void main(String[] args) throws Exception {
        ConexionMySql conexiones = new ConexionMySql(ConfiguracionBaseDatos.desdeEntorno());
        ReservaTransaccionalRepositorio repositorio =
            new JdbcReservaTransaccionalRepositorio(conexiones);
        limpiar(conexiones);

        try {
            comprobar(repositorio.listar().size() == 2,
                "mapea las dos reservas iniciales");

            Reserva pendiente = repositorio.crearPendiente(new SolicitudReserva(
                1, 5, 60, FECHA_DESDE.atTime(18, 0), 1, AHORA));
            comprobar(pendiente.getEstado() == EstadoReserva.PENDIENTE
                    && pendiente.getPrecioTotal().compareTo(new BigDecimal("40000.00")) == 0
                    && pendiente.getSena().compareTo(new BigDecimal("8000.00")) == 0
                    && pendiente.getVencimiento().equals(AHORA.plusMinutes(30)),
                "registra una reserva pendiente con importes y vencimiento");

            ReservaTransaccionalRepositorio nuevaInstancia =
                new JdbcReservaTransaccionalRepositorio(conexiones);
            comprobar(nuevaInstancia.buscar(pendiente.getId()).map(Reserva::getInicio)
                    .orElseThrow().equals(FECHA_DESDE.atTime(18, 0)),
                "conserva la reserva al crear una nueva instancia");

            rechazaConflicto(() -> repositorio.crearPendiente(new SolicitudReserva(
                    2, 5, 60, FECHA_DESDE.atTime(18, 30), 1, AHORA)),
                "El horario está ocupado.", "rechaza una superposición parcial");

            Reserva contigua = repositorio.crearPendiente(new SolicitudReserva(
                2, 5, 60, FECHA_DESDE.atTime(19, 0), 1, AHORA));
            comprobar(contigua.getId() != pendiente.getId(),
                "permite un turno contiguo");

            Pago sena = repositorio.registrarSena(pendiente.getId(), 1, AHORA.plusMinutes(10));
            comprobar(sena.getTipo() == TipoPago.SENA
                    && sena.getImporte().compareTo(new BigDecimal("8000.00")) == 0
                    && repositorio.buscar(pendiente.getId()).orElseThrow().getEstado()
                        == EstadoReserva.CONFIRMADA,
                "registra la seña y confirma la reserva atómicamente");
            comprobar(repositorio.listarPagos(pendiente.getId()).size() == 1,
                "persiste y consulta el movimiento de pago");
            rechazaConflicto(() -> repositorio.registrarSena(
                    pendiente.getId(), 1, AHORA.plusMinutes(11)),
                "La seña ya fue cubierta.", "impide duplicar la seña");

            Reserva vencida = repositorio.crearPendiente(new SolicitudReserva(
                1, 3, 60, FECHA_DESDE.plusDays(1).atTime(18, 0), 1, AHORA));
            rechazaConflicto(() -> repositorio.registrarSena(
                    vencida.getId(), 1, AHORA.plusMinutes(30)),
                "Venció el plazo de 30 minutos para pagar la seña.",
                "persiste el vencimiento al intentar pagar fuera de término");
            comprobar(repositorio.buscar(vencida.getId()).orElseThrow().getEstado()
                    == EstadoReserva.CANCELADA,
                "el vencimiento queda persistido como cancelación");
            Reserva horarioLiberado = repositorio.crearPendiente(new SolicitudReserva(
                2, 3, 60, FECHA_DESDE.plusDays(1).atTime(18, 0), 1,
                AHORA.plusMinutes(31)));
            comprobar(horarioLiberado.getEstado() == EstadoReserva.PENDIENTE,
                "una reserva vencida libera el horario");

            comprobarCompetenciaConcurrente(conexiones);
        } finally {
            limpiar(conexiones);
        }

        comprobar(repositorio.listar().size() == 2,
            "la limpieza conserva únicamente los datos iniciales");
        System.out.println("Resultado reserva JDBC: " + verificaciones
            + " verificaciones aprobadas.");
    }

    private static void comprobarCompetenciaConcurrente(ConexionMySql conexiones) throws Exception {
        CountDownLatch preparados = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        ExecutorService ejecutor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Object>> tareas = List.of(
                tareaConcurrente(conexiones, 1, preparados, iniciar),
                tareaConcurrente(conexiones, 2, preparados, iniciar));
            List<Future<Object>> resultados = new ArrayList<>();
            for (Callable<Object> tarea : tareas) {
                resultados.add(ejecutor.submit(tarea));
            }
            preparados.await();
            iniciar.countDown();

            int exitos = 0;
            int conflictos = 0;
            for (Future<Object> resultado : resultados) {
                Object valor = resultado.get();
                if (valor instanceof Reserva) exitos++;
                if (valor instanceof ConflictoReservaException) conflictos++;
            }
            comprobar(exitos == 1 && conflictos == 1,
                "serializa dos intentos concurrentes sobre la misma cancha");
        } finally {
            ejecutor.shutdownNow();
        }
    }

    private static Callable<Object> tareaConcurrente(ConexionMySql conexiones, long clienteId,
                                                       CountDownLatch preparados,
                                                       CountDownLatch iniciar) {
        return () -> {
            ReservaTransaccionalRepositorio repositorio =
                new JdbcReservaTransaccionalRepositorio(conexiones);
            preparados.countDown();
            iniciar.await();
            try {
                return repositorio.crearPendiente(new SolicitudReserva(
                    clienteId, 2, 90, FECHA_HASTA.atTime(18, 0), 1, AHORA));
            } catch (ConflictoReservaException e) {
                return e;
            }
        };
    }

    private static void limpiar(ConexionMySql conexiones) throws SQLException {
        String pagos = """
            DELETE p FROM pago p
            JOIN reserva r ON r.id_reserva = p.id_reserva
            WHERE r.fecha BETWEEN ? AND ?
            """;
        String reservas = "DELETE FROM reserva WHERE fecha BETWEEN ? AND ?";
        try (Connection conexion = conexiones.abrir()) {
            conexion.setAutoCommit(false);
            try (PreparedStatement borrarPagos = conexion.prepareStatement(pagos);
                 PreparedStatement borrarReservas = conexion.prepareStatement(reservas)) {
                borrarPagos.setObject(1, FECHA_DESDE);
                borrarPagos.setObject(2, FECHA_HASTA);
                borrarPagos.executeUpdate();
                borrarReservas.setObject(1, FECHA_DESDE);
                borrarReservas.setObject(2, FECHA_HASTA);
                borrarReservas.executeUpdate();
                conexion.commit();
            } catch (SQLException e) {
                conexion.rollback();
                throw e;
            }
        }
    }
}
