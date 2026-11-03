package ar.arenasur.mvc;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Punto de entrada JavaFX; la consola existente sigue siendo independiente. */
public final class ArenaSurApplication extends Application {
    @Override
    public void start(Stage escenario) {
        InicioController controlador = new InicioController();
        InicioView vista = new InicioView(controlador);
        controlador.cargarCatalogo();
        escenario.setTitle("Arena Sur");
        Scene escena = new Scene(vista, 960, 650);
        var estilo = getClass().getResource("/ar/arenasur/mvc/arena-sur.css");
        if (estilo != null) escena.getStylesheets().add(estilo.toExternalForm());
        escenario.setScene(escena);
        escenario.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
