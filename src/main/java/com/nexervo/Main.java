package com.nexervo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Carga el archivo desde la raíz de resources
        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("/gestiondereservas.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("NEXERVO - Gestión de Reservas");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}