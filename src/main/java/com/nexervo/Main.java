package com.nexervo;

import com.nexervo.api.ApiServer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Clase principal de JavaFX.
 * Carga la pantalla de login y configura la ventana principal.
 *
 * Incluye un manejador global de excepciones: cualquier RuntimeException
 * no capturada (p.ej. fallo de conexión a BD) muestra un Alert en lugar
 * de silenciarse en la consola o cerrar la app sin aviso.
 *
 * Aquí también arranco el ApiServer — el servidor HTTP embebido que sirve
 * la interfaz web para tablet/móvil. Lo hago en el start() porque quiero
 * que arranque junto con la app y se detenga cuando el usuario cierra la ventana.
 * Si el puerto ya está en uso (por ejemplo, si hay otra instancia abierta)
 * lo registro en el log pero no impido que la app siga funcionando.
 */
public class Main extends Application {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    /** Servidor REST embebido — referencia para detenerlo limpiamente al cerrar. */
    private ApiServer apiServer;

    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public void start(Stage stage) throws IOException {
        // Manejador global — cubre fallos de BD y otros errores inesperados
        Thread.setDefaultUncaughtExceptionHandler((thread, ex) ->
            Platform.runLater(() -> mostrarErrorGlobal(ex))
        );

        // Arrancar la API REST en un hilo daemon (no bloquea JavaFX)
        iniciarApiServer();

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

    /**
     * Se llama cuando JavaFX cierra la ventana principal.
     * Detiene el servidor HTTP limpiamente antes de que la JVM termine.
     */
    @Override
    public void stop() {
        if (apiServer != null) {
            apiServer.detener();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Intenta crear y arrancar ApiServer.
     * Si el puerto 7070 ya está en uso (otra instancia abierta), registra la
     * advertencia en el log pero no impide que la app de escritorio arranque.
     */
    private void iniciarApiServer() {
        try {
            apiServer = new ApiServer();
            apiServer.iniciar();
        } catch (IOException e) {
            log.warn("No se pudo iniciar la API REST (puerto {} ocupado o error de red): {}",
                    ApiServer.PUERTO, e.getMessage());
            // La app de escritorio funciona con normalidad aunque la API no arranque
        }
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
