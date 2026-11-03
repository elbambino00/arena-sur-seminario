package ar.arenasur.mvc;

import ar.arenasur.modelo.Cancha;
import java.util.List;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

/** Vista inicial JavaFX; no accede directamente a repositorios. */
public final class InicioView extends BorderPane {
    private final InicioController controlador;
    private final Label estado = new Label();
    private final ListView<String> canchas = new ListView<>();
    private final ListView<String> tarifas = new ListView<>();
    private final ListView<String> clientes = new ListView<>();
    private final ListView<String> reservas = new ListView<>();

    public InicioView(InicioController controlador) {
        this.controlador = controlador;
        getStyleClass().add("root-pane");
        setPadding(new Insets(18));
        Label titulo = new Label("Arena Sur");
        titulo.getStyleClass().add("brand-title");
        Label subtitulo = new Label("Sistema de gestión de reservas  |  Complejo Deportivo");
        subtitulo.getStyleClass().add("brand-subtitle");
        Button actualizar = new Button("Actualizar catálogo");
        actualizar.setOnAction(event -> refrescar());
        estado.getStyleClass().add("status-label");
        actualizar.getStyleClass().add("primary-button");
        HBox identidad = new HBox(14, titulo, subtitulo);
        identidad.getStyleClass().add("brand-row");
        VBox encabezado = new VBox(8, identidad, estado, actualizar);
        encabezado.getStyleClass().add("header-pane");
        setTop(new VBox(encabezado, navegacion()));

        TabPane pestañas = new TabPane();
        Tab tabDisponibilidad = new Tab("Consultar disponibilidad", disponibilidadPane());
        Tab tabRegistrar = new Tab("Registrar reserva", reservasPane());
        Tab tabCanchas = new Tab("Canchas", canchas);
        Tab tabTarifas = new Tab("Tarifas", tarifas);
        Tab tabClientes = new Tab("Clientes", clientesPane());
        Tab tabOperaciones = new Tab("Pagos y cambios", operacionesPane());
        tabCanchas.setClosable(false);
        tabTarifas.setClosable(false);
        tabClientes.setClosable(false);
        tabDisponibilidad.setClosable(false);
        tabRegistrar.setClosable(false);
        tabOperaciones.setClosable(false);
        pestañas.getTabs().addAll(tabDisponibilidad, tabRegistrar, tabCanchas, tabTarifas,
            tabClientes, tabOperaciones);
        setCenter(pestañas);
        refrescar();
    }

    private void refrescar() {
        controlador.cargarCatalogo();
        canchas.getItems().setAll(controlador.canchas().stream().map(Object::toString).toList());
        tarifas.getItems().setAll(controlador.tarifas().stream()
            .map(t -> t.deporte() + " | " + t.duracion() + " min | $" + t.precio()).toList());
        clientes.getItems().setAll(controlador.clientes().stream()
            .map(c -> c.getId() + " | " + c.getNombreCompleto() + " | " + c.getEmail()).toList());
        reservas.getItems().setAll(controlador.reservas().stream().map(Object::toString).toList());
        estado.setText(controlador.mensaje());
    }

    private VBox clientesPane() {
        TextField nombre = new TextField(); nombre.setPromptText("Nombre");
        TextField apellido = new TextField(); apellido.setPromptText("Apellido");
        TextField telefono = new TextField(); telefono.setPromptText("Teléfono");
        TextField email = new TextField(); email.setPromptText("Email");
        Button guardar = new Button("Registrar cliente");
        guardar.setOnAction(event -> {
            if (controlador.registrarCliente(nombre.getText(), apellido.getText(),
                    telefono.getText(), email.getText())) {
                nombre.clear(); apellido.clear(); telefono.clear(); email.clear();
                refrescar();
            }
            estado.setText(controlador.mensaje());
        });
        GridPane formulario = new GridPane();
        formulario.setHgap(8); formulario.setVgap(8);
        formulario.addRow(0, nombre, apellido); formulario.addRow(1, telefono, email);
        return new VBox(10, formulario, guardar, clientes);
    }

    private HBox navegacion() {
        HBox barra = new HBox(18);
        barra.getStyleClass().add("navigation-bar");
        for (String etiqueta : new String[]{"Inicio", "Reservas", "Agenda", "Canchas", "Clientes", "Pagos", "Tarifas"}) {
            Label item = new Label(etiqueta);
            item.getStyleClass().add("navigation-item");
            barra.getChildren().add(item);
        }
        return barra;
    }

    private VBox reservasPane() {
        TextField cliente = new TextField(); cliente.setPromptText("ID cliente");
        TextField cancha = new TextField(); cancha.setPromptText("ID cancha");
        ComboBox<Integer> duracion = new ComboBox<>();
        duracion.getItems().addAll(60, 90); duracion.setValue(60);
        TextField inicio = new TextField(); inicio.setPromptText("AAAA-MM-DD HH:mm");
        Button guardar = new Button("Registrar reserva pendiente");
        guardar.setOnAction(event -> {
            try {
                if (controlador.registrarReserva(Long.parseLong(cliente.getText()),
                        Long.parseLong(cancha.getText()), duracion.getValue(), inicio.getText())) {
                    cliente.clear(); cancha.clear(); inicio.clear(); refrescar();
                }
            } catch (NumberFormatException e) {
                estado.setText("Los IDs deben ser numéricos.");
            }
            estado.setText(controlador.mensaje());
        });
        GridPane formulario = new GridPane();
        formulario.setHgap(8); formulario.setVgap(8);
        formulario.addRow(0, cliente, cancha, duracion);
        formulario.addRow(1, inicio, guardar);
        Label seccion = new Label("1. Datos del cliente y 2. Parámetros del turno");
        seccion.getStyleClass().add("section-title");
        Label liquidacion = new Label("3. Liquidación y condiciones de seña — la seña requerida es el 20% del total");
        liquidacion.getStyleClass().add("summary-card");
        return new VBox(10, seccion, formulario, liquidacion, guardar, reservas);
    }

    private VBox disponibilidadPane() {
        ComboBox<String> deporte = new ComboBox<>();
        deporte.getItems().addAll("Todos", "Fútbol 5", "Pádel", "Tenis"); deporte.setValue("Fútbol 5");
        TextField fecha = new TextField(); fecha.setPromptText("AAAA-MM-DD");
        TextField hora = new TextField(); hora.setPromptText("HH:mm");
        ComboBox<Integer> duracion = new ComboBox<>(); duracion.getItems().addAll(60, 90); duracion.setValue(60);
        ListView<String> resultados = new ListView<>();
        Button consultar = new Button("Consultar disponibilidad"); consultar.getStyleClass().add("primary-button");
        consultar.setOnAction(e -> {
            List<Cancha> libres = controlador.disponibles(deporte.getValue(), fecha.getText(), hora.getText(), duracion.getValue());
            resultados.getItems().setAll(libres.stream().map(c -> "◉  " + c.getNombre()
                + "  |  " + c.getDeporte() + "  |  " + duracion.getValue() + " min  |  DISPONIBLE").toList());
            estado.setText(controlador.mensaje());
        });
        GridPane filtros = new GridPane(); filtros.setHgap(10); filtros.setVgap(8);
        filtros.addRow(0, new Label("DEPORTE"), deporte, new Label("FECHA DEL TURNO"), fecha,
            new Label("HORARIO DESEADO"), hora, new Label("DURACIÓN"), duracion, consultar);
        Label filtrosActivos = new Label("Filtros activos · seleccione una cancha disponible para continuar");
        filtrosActivos.getStyleClass().add("info-card");
        Label seleccionado = new Label("Turno preseleccionado: —       Total: —");
        seleccionado.getStyleClass().add("summary-card");
        return new VBox(12, new Label("Consultar disponibilidad"), filtros, filtrosActivos,
            resultados, seleccionado);
    }

    private VBox operacionesPane() {
        TextField reserva = new TextField(); reserva.setPromptText("ID reserva");
        TextField cancha = new TextField(); cancha.setPromptText("Nueva cancha");
        TextField inicio = new TextField(); inicio.setPromptText("Nuevo inicio AAAA-MM-DD HH:mm");
        ComboBox<Integer> duracion = new ComboBox<>(); duracion.getItems().addAll(60, 90); duracion.setValue(60);
        Button sena = new Button("Registrar seña");
        Button saldo = new Button("Registrar saldo");
        Button modificar = new Button("Modificar reserva");
        Button cancelar = new Button("Cancelar reserva");
        ListView<String> pagos = new ListView<>();
        Runnable refrescarPagos = () -> {
            try { pagos.getItems().setAll(controlador.pagos(Long.parseLong(reserva.getText()))
                .stream().map(Object::toString).toList()); }
            catch (NumberFormatException e) { estado.setText("El ID de reserva debe ser numérico."); }
        };
        sena.setOnAction(e -> { if (ejecutar(() -> controlador.registrarSena(Long.parseLong(reserva.getText())))) refrescar(); });
        saldo.setOnAction(e -> { if (ejecutar(() -> controlador.registrarSaldo(Long.parseLong(reserva.getText())))) refrescar(); });
        modificar.setOnAction(e -> { if (ejecutar(() -> controlador.modificarReserva(
            Long.parseLong(reserva.getText()), Long.parseLong(cancha.getText()), duracion.getValue(), inicio.getText()))) refrescar(); });
        cancelar.setOnAction(e -> { if (ejecutar(() -> controlador.cancelarReserva(Long.parseLong(reserva.getText())))) refrescar(); });
        Button consultar = new Button("Consultar pagos"); consultar.setOnAction(e -> refrescarPagos.run());
        GridPane formulario = new GridPane(); formulario.setHgap(8); formulario.setVgap(8);
        formulario.addRow(0, reserva, sena, saldo, cancelar);
        formulario.addRow(1, cancha, duracion, inicio, modificar);
        return new VBox(10, formulario, consultar, pagos);
    }

    private boolean ejecutar(java.util.function.BooleanSupplier accion) {
        try { boolean resultado = accion.getAsBoolean(); estado.setText(controlador.mensaje()); return resultado; }
        catch (NumberFormatException e) { estado.setText("El ID debe ser numérico."); return false; }
    }
}
