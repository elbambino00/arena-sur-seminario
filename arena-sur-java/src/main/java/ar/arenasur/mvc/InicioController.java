package ar.arenasur.mvc;

import ar.arenasur.consola.ContextoAplicacionJdbc;
import ar.arenasur.modelo.Cancha;
import ar.arenasur.modelo.Cliente;
import ar.arenasur.modelo.DatosCliente;
import ar.arenasur.modelo.Reserva;
import ar.arenasur.modelo.Pago;
import ar.arenasur.modelo.ModificacionReserva;
import ar.arenasur.modelo.SolicitudReserva;
import ar.arenasur.modelo.Tarifa;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;
import java.util.List;
import java.time.LocalDate;

/** Controlador inicial: coordina la lectura JDBC sin contener componentes visuales. */
public final class InicioController {
    private final ContextoAplicacionJdbc contexto;
    private List<Cancha> canchas = List.of();
    private List<Tarifa> tarifas = List.of();
    private List<Cliente> clientes = List.of();
    private List<Reserva> reservas = List.of();
    private String mensaje = "";
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm");

    public InicioController() {
        ContextoAplicacionJdbc creado = null;
        try {
            if (System.getenv("ARENA_SUR_DB_URL") != null) {
                creado = ContextoAplicacionJdbc.desdeEntorno();
            } else {
                mensaje = "Configure ARENA_SUR_DB_URL para usar la interfaz persistente.";
            }
        } catch (RuntimeException e) {
            mensaje = e.getMessage();
        }
        contexto = creado;
    }

    public void cargarCatalogo() {
        if (contexto == null) return;
        try {
            canchas = contexto.canchas().listarActivas();
            tarifas = contexto.tarifas().listarActivas();
            clientes = contexto.clientes().listarActivos();
            reservas = contexto.reservas().listar();
            mensaje = "Catálogo cargado correctamente.";
        } catch (RuntimeException e) {
            mensaje = e.getMessage();
        }
    }

    public List<Cancha> canchas() { return canchas; }
    public List<Tarifa> tarifas() { return tarifas; }
    public List<Cliente> clientes() { return clientes; }
    public List<Reserva> reservas() { return reservas; }
    public String mensaje() { return mensaje; }

    public boolean guardarCancha(String nombre, String deporte) {
        try { contexto.canchas().guardar(nombre, deporte); cargarCatalogo(); return true; }
        catch (RuntimeException e) { mensaje = e.getMessage(); return false; }
    }
    public boolean modificarCancha(long id, String nombre) {
        try { contexto.canchas().modificarNombre(id, nombre); cargarCatalogo(); return true; }
        catch (RuntimeException e) { mensaje = e.getMessage(); return false; }
    }
    public boolean eliminarCancha(long id) {
        try { contexto.canchas().desactivar(id); cargarCatalogo(); return true; }
        catch (RuntimeException e) { mensaje = e.getMessage(); return false; }
    }

    public List<Cancha> disponibles(String deporte, String fecha, String hora, int duracion) {
        try {
            LocalDateTime inicio = LocalDateTime.parse(fecha + " " + hora, FORMATO);
            LocalDateTime fin = inicio.plusMinutes(duracion);
            List<Cancha> resultado = canchas.stream()
                .filter(c -> deporte == null || deporte.isBlank() || c.getDeporte().equalsIgnoreCase(deporte))
                .filter(c -> reservas.stream().noneMatch(r -> r.seSuperpone(c.getId(), inicio, fin)))
                .collect(Collectors.toList());
            mensaje = resultado.size() + " canchas disponibles.";
            return resultado;
        } catch (RuntimeException e) { mensaje = e.getMessage(); return List.of(); }
    }

    public List<String> agenda(String fecha, String deporte) {
        try {
            LocalDate dia = LocalDate.parse(fecha);
            return reservas.stream().filter(r -> r.getInicio().toLocalDate().equals(dia))
                .filter(r -> deporte == null || deporte.isBlank() || r.getCancha().getDeporte().equalsIgnoreCase(deporte))
                .map(r -> r.getCancha().getNombre() + " | " + r.getInicio().toLocalTime()
                    + " - " + r.getFin().toLocalTime() + " | " + r.getEstado()
                    + " | Reserva " + r.getId()).toList();
        } catch (RuntimeException e) { mensaje = e.getMessage(); return List.of(); }
    }

    public List<Reserva> consultar(String cliente, String cancha, String fecha, String estado) {
        return reservas.stream()
            .filter(r -> cliente == null || cliente.isBlank() || r.getCliente().getNombreCompleto().toLowerCase().contains(cliente.toLowerCase()))
            .filter(r -> cancha == null || cancha.isBlank() || r.getCancha().getNombre().toLowerCase().contains(cancha.toLowerCase()))
            .filter(r -> fecha == null || fecha.isBlank() || r.getInicio().toLocalDate().toString().equals(fecha))
            .filter(r -> estado == null || estado.isBlank() || r.getEstado().name().equalsIgnoreCase(estado))
            .toList();
    }

    public boolean registrarCliente(String nombre, String apellido, String telefono, String email) {
        try {
            contexto.clientes().guardar(new DatosCliente(nombre, apellido, telefono, email));
            cargarCatalogo();
            return true;
        } catch (RuntimeException e) {
            mensaje = e.getMessage();
            return false;
        }
    }

    public boolean registrarReserva(long clienteId, long canchaId, int duracion, String inicio) {
        try {
            LocalDateTime comienzo = LocalDateTime.parse(inicio, FORMATO);
            contexto.reservas().crearPendiente(new SolicitudReserva(clienteId, canchaId, duracion,
                comienzo, 1L, LocalDateTime.now()));
            cargarCatalogo();
            return true;
        } catch (RuntimeException e) {
            mensaje = e.getMessage();
            return false;
        }
    }

    public boolean registrarSena(long reservaId) {
        return ejecutarPago(() -> contexto.reservas().registrarSena(reservaId, 1L, LocalDateTime.now()));
    }

    public boolean registrarSaldo(long reservaId) {
        return ejecutarPago(() -> contexto.reservas().registrarSaldo(reservaId, 1L, LocalDateTime.now()));
    }

    public boolean modificarReserva(long reservaId, long canchaId, int duracion, String inicio) {
        try {
            contexto.reservas().modificar(reservaId, new ModificacionReserva(canchaId, duracion,
                LocalDateTime.parse(inicio, FORMATO), LocalDateTime.now()));
            cargarCatalogo();
            return true;
        } catch (RuntimeException e) { mensaje = e.getMessage(); return false; }
    }

    public boolean cancelarReserva(long reservaId) {
        try {
            contexto.reservas().cancelar(reservaId, 1L, LocalDateTime.now());
            cargarCatalogo();
            return true;
        } catch (RuntimeException e) { mensaje = e.getMessage(); return false; }
    }

    public List<Pago> pagos(long reservaId) {
        try { return contexto.reservas().listarPagos(reservaId); }
        catch (RuntimeException e) { mensaje = e.getMessage(); return List.of(); }
    }

    private boolean ejecutarPago(java.util.function.Supplier<Pago> operacion) {
        try { operacion.get(); cargarCatalogo(); return true; }
        catch (RuntimeException e) { mensaje = e.getMessage(); return false; }
    }
}
