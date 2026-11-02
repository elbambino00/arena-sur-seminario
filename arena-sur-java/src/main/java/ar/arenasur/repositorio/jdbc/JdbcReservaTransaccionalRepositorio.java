package ar.arenasur.repositorio.jdbc;

import ar.arenasur.config.ConexionMySql;
import ar.arenasur.modelo.Cancha;
import ar.arenasur.modelo.CanchaFutbol;
import ar.arenasur.modelo.CanchaPadel;
import ar.arenasur.modelo.CanchaTenis;
import ar.arenasur.modelo.Cliente;
import ar.arenasur.modelo.EstadoReserva;
import ar.arenasur.modelo.Pago;
import ar.arenasur.modelo.Reserva;
import ar.arenasur.modelo.SolicitudReserva;
import ar.arenasur.modelo.Tarifa;
import ar.arenasur.modelo.TipoPago;
import ar.arenasur.repositorio.ConflictoReservaException;
import ar.arenasur.repositorio.PersistenciaException;
import ar.arenasur.repositorio.ReservaTransaccionalRepositorio;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcReservaTransaccionalRepositorio implements ReservaTransaccionalRepositorio {
    private static final String RESERVA_COMPLETA = """
        SELECT r.id_reserva, r.id_usuario, r.fecha, r.hora_inicio,
               r.duracion_minutos, r.estado, r.precio_total, r.importe_sena,
               r.credito_favor, r.fecha_hora_creacion, r.vencimiento_sena,
               c.id_cliente, c.nombre AS cliente_nombre, c.apellido AS cliente_apellido,
               c.telefono, c.email,
               ca.id_cancha, ca.nombre AS cancha_nombre, d.nombre AS deporte,
               t.id_tarifa
        FROM reserva r
        JOIN cliente c ON c.id_cliente = r.id_cliente
        JOIN cancha ca ON ca.id_cancha = r.id_cancha
        JOIN deporte d ON d.id_deporte = ca.id_deporte
        JOIN tarifa t ON t.id_tarifa = r.id_tarifa
        """;
    private static final String INSERTAR_RESERVA = """
        INSERT INTO reserva (
            id_cliente, id_cancha, id_tarifa, id_usuario, fecha, hora_inicio,
            duracion_minutos, estado, precio_total, importe_sena, credito_favor,
            fecha_hora_creacion, vencimiento_sena)
        VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDIENTE', ?, ?, 0.00, ?, ?)
        """;

    private final ConexionMySql conexiones;

    public JdbcReservaTransaccionalRepositorio(ConexionMySql conexiones) {
        this.conexiones = conexiones;
    }

    @Override
    public Reserva crearPendiente(SolicitudReserva solicitud) {
        try (Connection conexion = conexiones.abrir()) {
            conexion.setAutoCommit(false);
            try {
                Cliente cliente = buscarClienteActivo(conexion, solicitud.clienteId());
                Cancha cancha = bloquearCanchaActiva(conexion, solicitud.canchaId());
                Tarifa tarifa = buscarTarifaActiva(conexion, cancha.getDeporte(), solicitud.duracion());

                cancelarPendientesVencidas(conexion, solicitud.canchaId(),
                    solicitud.inicio().toLocalDate(), solicitud.fechaHoraCreacion());
                if (existeSuperposicion(conexion, solicitud)) {
                    throw new ConflictoReservaException("El horario está ocupado.");
                }

                BigDecimal importeSena = tarifa.precio().multiply(new BigDecimal("0.20"))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
                LocalDateTime vencimiento = solicitud.fechaHoraCreacion().plusMinutes(30);
                long id = insertarReserva(conexion, solicitud, tarifa, importeSena, vencimiento);
                Reserva reserva = new Reserva(id, solicitud.usuarioId(), cliente, cancha, tarifa,
                    solicitud.inicio(), solicitud.fechaHoraCreacion());
                conexion.commit();
                return reserva;
            } catch (SQLException | RuntimeException e) {
                rollbackSilencioso(conexion);
                if (e instanceof PersistenciaException persistencia) {
                    throw persistencia;
                }
                if (e instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new PersistenciaException("No se pudo registrar la reserva.", e);
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo iniciar la transacción de reserva.", e);
        }
    }

    @Override
    public Optional<Reserva> buscar(long id) {
        String sql = RESERVA_COMPLETA + " WHERE r.id_reserva = ?";
        try (Connection conexion = conexiones.abrir();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setLong(1, id);
            try (ResultSet resultados = consulta.executeQuery()) {
                return resultados.next() ? Optional.of(mapearReserva(resultados)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo consultar la reserva.", e);
        }
    }

    @Override
    public List<Reserva> listar() {
        String sql = RESERVA_COMPLETA + " ORDER BY r.fecha, r.hora_inicio, r.id_reserva";
        List<Reserva> reservas = new ArrayList<>();
        try (Connection conexion = conexiones.abrir();
             PreparedStatement consulta = conexion.prepareStatement(sql);
             ResultSet resultados = consulta.executeQuery()) {
            while (resultados.next()) {
                reservas.add(mapearReserva(resultados));
            }
            return List.copyOf(reservas);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar las reservas.", e);
        }
    }

    @Override
    public Pago registrarSena(long reservaId, long usuarioId, LocalDateTime fechaHora) {
        if (reservaId <= 0 || usuarioId <= 0 || fechaHora == null) {
            throw new IllegalArgumentException("Datos de pago inválidos.");
        }
        try (Connection conexion = conexiones.abrir()) {
            conexion.setAutoCommit(false);
            try {
                Reserva reserva = bloquearReserva(conexion, reservaId);
                if (reserva.getEstado() == EstadoReserva.PENDIENTE
                        && !fechaHora.isBefore(reserva.getVencimiento())) {
                    actualizarEstado(conexion, reservaId, EstadoReserva.CANCELADA);
                    conexion.commit();
                    throw new ConflictoReservaException(
                        "Venció el plazo de 30 minutos para pagar la seña.");
                }
                if (reserva.getEstado() != EstadoReserva.PENDIENTE
                        && reserva.getEstado() != EstadoReserva.CONFIRMADA) {
                    throw new ConflictoReservaException("La reserva no admite pagos de seña.");
                }

                BigDecimal abonado = totalPagado(conexion, reservaId, TipoPago.SENA);
                BigDecimal diferencia = reserva.getSena().subtract(abonado);
                if (diferencia.signum() <= 0) {
                    throw new ConflictoReservaException("La seña ya fue cubierta.");
                }

                Pago pago = insertarPago(conexion, reservaId, usuarioId, TipoPago.SENA,
                    diferencia, fechaHora);
                if (reserva.getEstado() == EstadoReserva.PENDIENTE) {
                    actualizarEstado(conexion, reservaId, EstadoReserva.CONFIRMADA);
                    reserva.confirmar();
                }
                conexion.commit();
                return pago;
            } catch (SQLException | RuntimeException e) {
                rollbackSilencioso(conexion);
                if (e instanceof PersistenciaException persistencia) {
                    throw persistencia;
                }
                if (e instanceof RuntimeException runtime) {
                    throw runtime;
                }
                throw new PersistenciaException("No se pudo registrar la seña.", e);
            }
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudo iniciar la transacción de pago.", e);
        }
    }

    @Override
    public List<Pago> listarPagos(long reservaId) {
        String sql = """
            SELECT id_pago, id_reserva, id_usuario, tipo, importe, fecha_hora
            FROM pago
            WHERE id_reserva = ?
            ORDER BY fecha_hora, id_pago
            """;
        List<Pago> pagos = new ArrayList<>();
        try (Connection conexion = conexiones.abrir();
             PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setLong(1, reservaId);
            try (ResultSet resultados = consulta.executeQuery()) {
                while (resultados.next()) {
                    pagos.add(mapearPago(resultados));
                }
            }
            return List.copyOf(pagos);
        } catch (SQLException e) {
            throw new PersistenciaException("No se pudieron consultar los pagos.", e);
        }
    }

    private Cliente buscarClienteActivo(Connection conexion, long id) throws SQLException {
        String sql = """
            SELECT id_cliente, nombre, apellido, telefono, email
            FROM cliente
            WHERE id_cliente = ? AND activo = TRUE
            """;
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setLong(1, id);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    throw new ConflictoReservaException("Cliente inexistente o inactivo.");
                }
                return new Cliente(resultado.getLong("id_cliente"), resultado.getString("nombre"),
                    resultado.getString("apellido"), resultado.getString("telefono"),
                    resultado.getString("email"));
            }
        }
    }

    private Cancha bloquearCanchaActiva(Connection conexion, long id) throws SQLException {
        String sql = """
            SELECT c.id_cancha, c.nombre, d.nombre AS deporte
            FROM cancha c
            JOIN deporte d ON d.id_deporte = c.id_deporte
            WHERE c.id_cancha = ? AND c.activa = TRUE AND d.activo = TRUE
            FOR UPDATE
            """;
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setLong(1, id);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    throw new ConflictoReservaException("Cancha inexistente o inactiva.");
                }
                return crearCancha(resultado.getLong("id_cancha"),
                    resultado.getString("nombre"), resultado.getString("deporte"));
            }
        }
    }

    private Tarifa buscarTarifaActiva(Connection conexion, String deporte, int duracion)
            throws SQLException {
        String sql = """
            SELECT t.id_tarifa, d.nombre AS deporte, t.duracion_minutos, t.precio
            FROM tarifa t
            JOIN deporte d ON d.id_deporte = t.id_deporte
            WHERE d.nombre = ? AND t.duracion_minutos = ?
              AND t.activa = TRUE AND d.activo = TRUE
            """;
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setString(1, deporte);
            consulta.setInt(2, duracion);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    throw new ConflictoReservaException("No existe una tarifa activa para el turno.");
                }
                return new Tarifa(resultado.getLong("id_tarifa"), resultado.getString("deporte"),
                    resultado.getInt("duracion_minutos"), resultado.getBigDecimal("precio"));
            }
        }
    }

    private void cancelarPendientesVencidas(Connection conexion, long canchaId, LocalDate fecha,
                                             LocalDateTime ahora) throws SQLException {
        String sql = """
            UPDATE reserva
            SET estado = 'CANCELADA'
            WHERE id_cancha = ? AND fecha = ? AND estado = 'PENDIENTE'
              AND vencimiento_sena <= ?
            """;
        try (PreparedStatement actualizacion = conexion.prepareStatement(sql)) {
            actualizacion.setLong(1, canchaId);
            actualizacion.setObject(2, fecha);
            actualizacion.setObject(3, ahora);
            actualizacion.executeUpdate();
        }
    }

    private boolean existeSuperposicion(Connection conexion, SolicitudReserva solicitud)
            throws SQLException {
        String sql = """
            SELECT id_reserva
            FROM reserva
            WHERE id_cancha = ? AND fecha = ?
              AND estado IN ('PENDIENTE', 'CONFIRMADA')
              AND ? < ADDTIME(hora_inicio, SEC_TO_TIME(duracion_minutos * 60))
              AND ? > hora_inicio
            LIMIT 1
            FOR UPDATE
            """;
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setLong(1, solicitud.canchaId());
            consulta.setObject(2, solicitud.inicio().toLocalDate());
            consulta.setObject(3, solicitud.inicio().toLocalTime());
            consulta.setObject(4, solicitud.inicio().plusMinutes(solicitud.duracion()).toLocalTime());
            try (ResultSet resultado = consulta.executeQuery()) {
                return resultado.next();
            }
        }
    }

    private long insertarReserva(Connection conexion, SolicitudReserva solicitud, Tarifa tarifa,
                                 BigDecimal importeSena, LocalDateTime vencimiento) throws SQLException {
        try (PreparedStatement insercion = conexion.prepareStatement(
                INSERTAR_RESERVA, Statement.RETURN_GENERATED_KEYS)) {
            insercion.setLong(1, solicitud.clienteId());
            insercion.setLong(2, solicitud.canchaId());
            insercion.setLong(3, tarifa.id());
            insercion.setLong(4, solicitud.usuarioId());
            insercion.setObject(5, solicitud.inicio().toLocalDate());
            insercion.setObject(6, solicitud.inicio().toLocalTime());
            insercion.setInt(7, solicitud.duracion());
            insercion.setBigDecimal(8, tarifa.precio());
            insercion.setBigDecimal(9, importeSena);
            insercion.setObject(10, solicitud.fechaHoraCreacion());
            insercion.setObject(11, vencimiento);
            insercion.executeUpdate();
            try (ResultSet claves = insercion.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new PersistenciaException("MySQL no devolvió el ID de la reserva.");
                }
                return claves.getLong(1);
            }
        }
    }

    private Reserva bloquearReserva(Connection conexion, long id) throws SQLException {
        String sql = RESERVA_COMPLETA + " WHERE r.id_reserva = ? FOR UPDATE";
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setLong(1, id);
            try (ResultSet resultado = consulta.executeQuery()) {
                if (!resultado.next()) {
                    throw new ConflictoReservaException("Reserva inexistente.");
                }
                return mapearReserva(resultado);
            }
        }
    }

    private BigDecimal totalPagado(Connection conexion, long reservaId, TipoPago tipo)
            throws SQLException {
        String sql = "SELECT COALESCE(SUM(importe), 0) FROM pago WHERE id_reserva = ? AND tipo = ?";
        try (PreparedStatement consulta = conexion.prepareStatement(sql)) {
            consulta.setLong(1, reservaId);
            consulta.setString(2, tipo.name());
            try (ResultSet resultado = consulta.executeQuery()) {
                resultado.next();
                return resultado.getBigDecimal(1);
            }
        }
    }

    private Pago insertarPago(Connection conexion, long reservaId, long usuarioId, TipoPago tipo,
                              BigDecimal importe, LocalDateTime fechaHora) throws SQLException {
        String sql = """
            INSERT INTO pago (id_reserva, id_usuario, tipo, importe, fecha_hora)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (PreparedStatement insercion = conexion.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            insercion.setLong(1, reservaId);
            insercion.setLong(2, usuarioId);
            insercion.setString(3, tipo.name());
            insercion.setBigDecimal(4, importe);
            insercion.setObject(5, fechaHora);
            insercion.executeUpdate();
            try (ResultSet claves = insercion.getGeneratedKeys()) {
                if (!claves.next()) {
                    throw new PersistenciaException("MySQL no devolvió el ID del pago.");
                }
                return new Pago(claves.getLong(1), reservaId, usuarioId, tipo, importe, fechaHora);
            }
        }
    }

    private void actualizarEstado(Connection conexion, long reservaId, EstadoReserva estado)
            throws SQLException {
        try (PreparedStatement actualizacion = conexion.prepareStatement(
                "UPDATE reserva SET estado = ? WHERE id_reserva = ?")) {
            actualizacion.setString(1, estado.name());
            actualizacion.setLong(2, reservaId);
            actualizacion.executeUpdate();
        }
    }

    private Reserva mapearReserva(ResultSet resultado) throws SQLException {
        Cliente cliente = new Cliente(resultado.getLong("id_cliente"),
            resultado.getString("cliente_nombre"), resultado.getString("cliente_apellido"),
            resultado.getString("telefono"), resultado.getString("email"));
        Cancha cancha = crearCancha(resultado.getLong("id_cancha"),
            resultado.getString("cancha_nombre"), resultado.getString("deporte"));
        Tarifa tarifaHistorica = new Tarifa(resultado.getLong("id_tarifa"),
            resultado.getString("deporte"), resultado.getInt("duracion_minutos"),
            resultado.getBigDecimal("precio_total"));
        LocalDateTime inicio = LocalDateTime.of(
            resultado.getObject("fecha", LocalDate.class),
            resultado.getObject("hora_inicio", LocalTime.class));
        return Reserva.rehidratar(resultado.getLong("id_reserva"),
            resultado.getLong("id_usuario"), cliente, cancha, tarifaHistorica, inicio,
            resultado.getObject("fecha_hora_creacion", LocalDateTime.class),
            resultado.getObject("vencimiento_sena", LocalDateTime.class),
            EstadoReserva.valueOf(resultado.getString("estado")),
            resultado.getBigDecimal("precio_total"), resultado.getBigDecimal("importe_sena"),
            resultado.getBigDecimal("credito_favor"));
    }

    private Pago mapearPago(ResultSet resultado) throws SQLException {
        return new Pago(resultado.getLong("id_pago"), resultado.getLong("id_reserva"),
            resultado.getLong("id_usuario"), TipoPago.valueOf(resultado.getString("tipo")),
            resultado.getBigDecimal("importe"),
            resultado.getObject("fecha_hora", LocalDateTime.class));
    }

    private Cancha crearCancha(long id, String nombre, String deporte) {
        return switch (deporte) {
            case "Fútbol 5" -> new CanchaFutbol(id, nombre);
            case "Pádel" -> new CanchaPadel(id, nombre);
            case "Tenis" -> new CanchaTenis(id, nombre);
            default -> throw new PersistenciaException("Deporte de cancha no soportado: " + deporte);
        };
    }

    private void rollbackSilencioso(Connection conexion) {
        try {
            conexion.rollback();
        } catch (SQLException ignorada) {
            // Se conserva la excepción original.
        }
    }
}
