package com.nexervo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Clase principal de JavaFX.
 * Carga la pantalla de login y configura la ventana principal.
 *
 * Incluye un manejador global de excepciones: cualquier RuntimeException
 * no capturada (p.ej. fallo de conexión a BD) muestra un Alert en lugar
 * de silenciarse en la consola o cerrar la app sin aviso.
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Manejador global — cubre fallos de BD y otros errores inesperados
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) ->
            Platform.runLater(() -> mostrarErrorGlobal(ex))
        );

        FXMLLoader loader = new FXMLLoader(
            Main.class.getResource("/vista/login.fxml")
        );
        Scene scene = new Scene(loader.load());

        stage.setTitle("NEXERVO · Sistema de Gestión de Reservas");
        stage.setScene(scene);
        stage.setWidth(860);
        stage.setHeight(560);
        stage.setMinWidth(640);
        stage.setMinHeight(480);
        stage.setResizable(true);
        stage.centerOnScreen();
        stage.show();
    }

    private static void mostrarErrorGlobal(Throwable ex) {
        String mensaje = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("NEXERVO — Error");
        alert.setHeaderText("Se produjo un error inesperado");
        alert.setContentText(mensaje + "\n\nRevisa que MySQL esté arrancado y que config.properties sea correcto.");
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
