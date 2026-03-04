package com.nexervo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * Clase principal que extiende de Application.
 * Se encarga de cargar la vista FXML y configurar el Stage (ventana).
 */
public class Main extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        // Carga el archivo FXML desde la ruta de recursos
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/com/nexervo/gestiondereservas.fxml"));

        // Crea la escena con el contenido del FXML
        Scene scene = new Scene(fxmlLoader.load());

        // Configuración básica de la ventana
        stage.setTitle("NEXERVO - Gestión de Reservas");
        stage.setScene(scene);

        // Desactiva la dimensión para el diseño estático
        stage.setResizable(false);

        stage.show();
    }

    /**
     * Método main estándar para lanzar la aplicación JavaFX.
     */
    public static void main(String[] args) {
        launch();
    }
}