package com.nexervo.controlador;

import com.nexervo.Main;
import com.nexervo.modelo.Usuario;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import java.io.IOException;

public class PrincipalControlador {

    @FXML private BorderPane panelRaiz;
    @FXML private Label      lblUsuario;
    @FXML private Button     btnNuevaReserva;
    @FXML private Button     btnGestionReservas;
    @FXML private Button     btnClientes;
    @FXML private Button     btnPagos;
    @FXML private Button     btnGestionEmpleados;
    @FXML private Button     btnDashboard;

    private Usuario usuarioActivo;

    public interface NecesitaUsuario { void setUsuario(Usuario u); }

    public void inicializar(Usuario usuario) {
        this.usuarioActivo = usuario;
        lblUsuario.setText(usuario.getNombre() + "  ·  " + usuario.getRol());

        // Pagos, gestión de empleados y estadísticas solo para ADMIN
        btnPagos.setVisible(usuario.esAdmin());
        btnPagos.setManaged(usuario.esAdmin());

        if (btnGestionEmpleados != null) {
            btnGestionEmpleados.setVisible(usuario.esAdmin());
            btnGestionEmpleados.setManaged(usuario.esAdmin());
        }
        if (btnDashboard != null) {
            btnDashboard.setVisible(usuario.esAdmin());
            btnDashboard.setManaged(usuario.esAdmin());
        }

        cargar("/vista/nueva_reserva.fxml", btnNuevaReserva);
    }

    @FXML public void onNuevaReserva()    { cargar("/vista/nueva_reserva.fxml",    btnNuevaReserva); }
    @FXML public void onGestionReservas() { cargar("/vista/gestion_reservas.fxml", btnGestionReservas); }
    @FXML public void onClientes()        { cargar("/vista/clientes.fxml",          btnClientes); }
    @FXML public void onPagos()           { cargar("/vista/pagos.fxml",             btnPagos); }
    @FXML public void onDashboard()       { cargar("/vista/dashboard.fxml",         btnDashboard); }

    /** Carga la pantalla de gestión de camareros */
    @FXML
    public void onGestionEmpleados() {
        cargar("/vista/camareros.fxml", btnGestionEmpleados);
    }

    @FXML
    public void onCerrarSesion() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/vista/login.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load());
            Stage stage = (Stage) panelRaiz.getScene().getWindow();
            stage.setTitle("NEXERVO · Acceso");
            stage.setScene(scene);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void cargar(String fxml, Button botonActivo) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxml));
            Node vista = loader.load();
            Object ctrl = loader.getController();
            if (ctrl instanceof NecesitaUsuario nu) nu.setUsuario(usuarioActivo);
            panelRaiz.setCenter(vista);
            marcarActivo(botonActivo);
        } catch (IOException e) {
            System.err.println("[NEXERVO] Error cargando " + fxml + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void marcarActivo(Button activo) {
        for (Button b : new Button[]{btnNuevaReserva, btnGestionReservas,
                                     btnClientes, btnPagos, btnGestionEmpleados, btnDashboard}) {
            if (b == null) continue;
            b.getStyleClass().remove("nav-activo");
        }
        if (activo != null) activo.getStyleClass().add("nav-activo");
    }
}
