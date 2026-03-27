package com.nexervo.controlador;

import com.nexervo.Main;
import com.nexervo.datos.UsuarioDAO;
import com.nexervo.modelo.Usuario;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;

/**
 * LoginControlador V5
 *
 * FIX 1: El botón "Reservar una mesa" ahora está en el login.fxml
 *         y llama a onAccesoClienteClick() que abre cliente_reserva.fxml.
 *
 * ADD: mostrarDialogoGestionEmpleados() — gestión completa de camareros:
 *       crear, borrar, cambiar contraseña, activar/desactivar.
 */
public class LoginControlador {

    @FXML private TextField     txtUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private Label         lblError;
    @FXML private Button        btnEntrar;
    @FXML private Button        btnCliente;   // Botón acceso cliente

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    @FXML
    public void initialize() {
        lblError.setVisible(false);
        txtUsuario.setOnAction(e -> onEntrarClick());
        txtContrasena.setOnAction(e -> onEntrarClick());
    }

    // ── Acceso personal ───────────────────────────────────────────

    @FXML
    public void onEntrarClick() {
        String u = txtUsuario.getText().trim();
        String p = txtContrasena.getText();
        if (u.isEmpty() || p.isEmpty()) { mostrarError("Introduce usuario y contraseña."); return; }

        Usuario autenticado = usuarioDAO.autenticar(u, p);
        if (autenticado == null) {
            mostrarError("Usuario o contraseña incorrectos.");
            txtContrasena.clear();
            txtContrasena.requestFocus();
        } else {
            abrirPrincipal(autenticado);
        }
    }

    // ── Acceso cliente (sin contraseña) ───────────────────────────

    @FXML
    public void onAccesoClienteClick() {
        try {
            FXMLLoader loader = new FXMLLoader(
                Main.class.getResource("/vista/cliente_reserva.fxml")
            );
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) btnCliente.getScene().getWindow();
            stage.setTitle("NEXERVO · Reserva tu mesa");
            stage.setScene(scene);
            stage.setMinWidth(700);
            stage.setMinHeight(600);
        } catch (IOException e) {
            mostrarError("Error al abrir la vista de cliente.");
            e.printStackTrace();
        }
    }

    // ── Diálogo gestión de camareros (llamado desde PrincipalControlador) ──

    /**
     * Abre el diálogo completo de gestión de empleados.
     * Permite crear, borrar, cambiar contraseña y activar/desactivar.
     * Solo accesible para ADMIN.
     */
    public static void mostrarDialogoGestionEmpleados(Stage owner) {
        Stage dialogo = new Stage();
        dialogo.initOwner(owner);
        dialogo.setTitle("NEXERVO · Gestión de camareros");
        dialogo.setMinWidth(500);
        dialogo.setMinHeight(520);

        UsuarioDAO dao = new UsuarioDAO();

        // ── Tabla de empleados ──
        javafx.scene.control.TableView<Usuario> tabla = new javafx.scene.control.TableView<>();
        tabla.setPrefHeight(200);

        javafx.scene.control.TableColumn<Usuario, String> colNombre = new javafx.scene.control.TableColumn<>("Nombre");
        colNombre.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getNombre()));
        colNombre.setPrefWidth(150);

        javafx.scene.control.TableColumn<Usuario, String> colLogin = new javafx.scene.control.TableColumn<>("Usuario");
        colLogin.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getUsuario()));
        colLogin.setPrefWidth(120);

        javafx.scene.control.TableColumn<Usuario, String> colRol = new javafx.scene.control.TableColumn<>("Rol");
        colRol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getRol()));
        colRol.setPrefWidth(80);

        javafx.scene.control.TableColumn<Usuario, String> colActivo = new javafx.scene.control.TableColumn<>("Estado");
        colActivo.setCellValueFactory(c ->
            new javafx.beans.property.SimpleStringProperty(c.getValue().isActivo() ? "ACTIVO" : "INACTIVO"));
        colActivo.setPrefWidth(80);

        tabla.getColumns().addAll(colNombre, colLogin, colRol, colActivo);

        Runnable recargar = () -> {
            tabla.setItems(javafx.collections.FXCollections.observableArrayList(
                dao.listarEmpleados()
            ));
        };
        recargar.run();

        // ── Sección crear nuevo empleado ──
        Label lblCrear = new Label("Nuevo camarero:");
        lblCrear.setStyle("-fx-font-weight:bold; -fx-text-fill:#00C6FF;");
        TextField txtNombre   = new TextField(); txtNombre.setPromptText("Nombre completo");
        TextField txtLogin    = new TextField(); txtLogin.setPromptText("Usuario de acceso");
        PasswordField txtPass = new PasswordField(); txtPass.setPromptText("Contraseña");

        Button btnCrear = new Button("Crear camarero");
        btnCrear.setStyle("-fx-background-color:linear-gradient(to right,#6A11CB,#2575FC);" +
            "-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;-fx-cursor:hand;");
        btnCrear.setOnAction(e -> {
            if (txtNombre.getText().isBlank() || txtLogin.getText().isBlank() || txtPass.getText().isBlank()) {
                alerta("Todos los campos son obligatorios."); return;
            }
            if (dao.existeUsuario(txtLogin.getText().trim())) {
                alerta("Ese nombre de usuario ya existe."); return;
            }
            Usuario nuevo = new Usuario(txtNombre.getText().trim(),
                txtLogin.getText().trim(), txtPass.getText(), "EMPLEADO");
            if (dao.crearUsuario(nuevo)) {
                txtNombre.clear(); txtLogin.clear(); txtPass.clear();
                recargar.run();
            } else { alerta("No se pudo crear el camarero."); }
        });

        // ── Sección acciones sobre empleado seleccionado ──
        Label lblAcciones = new Label("Acciones sobre empleado seleccionado:");
        lblAcciones.setStyle("-fx-font-weight:bold; -fx-text-fill:#FFC107;");

        PasswordField txtNuevoPass = new PasswordField();
        txtNuevoPass.setPromptText("Nueva contraseña");

        Button btnCambiarPass = new Button("Cambiar contraseña");
        btnCambiarPass.setStyle("-fx-background-color:transparent;-fx-border-color:#5D5D8A;" +
            "-fx-border-radius:8;-fx-text-fill:#AAAACC;-fx-cursor:hand;");
        btnCambiarPass.setOnAction(e -> {
            Usuario sel = tabla.getSelectionModel().getSelectedItem();
            if (sel == null) { alerta("Selecciona un camarero de la tabla."); return; }
            if (txtNuevoPass.getText().isBlank()) { alerta("Introduce la nueva contraseña."); return; }
            if (dao.cambiarContrasena(sel.getIdUsuario(), txtNuevoPass.getText())) {
                txtNuevoPass.clear();
                alerta("Contraseña actualizada para " + sel.getUsuario() + ".");
            } else { alerta("No se pudo cambiar la contraseña."); }
        });

        Button btnToggleActivo = new Button("Activar / Desactivar");
        btnToggleActivo.setStyle("-fx-background-color:transparent;-fx-border-color:#5D5D8A;" +
            "-fx-border-radius:8;-fx-text-fill:#AAAACC;-fx-cursor:hand;");
        btnToggleActivo.setOnAction(e -> {
            Usuario sel = tabla.getSelectionModel().getSelectedItem();
            if (sel == null) { alerta("Selecciona un camarero de la tabla."); return; }
            boolean nuevoEstado = !sel.isActivo();
            if (dao.cambiarEstadoUsuario(sel.getIdUsuario(), nuevoEstado)) {
                recargar.run();
            } else { alerta("No se pudo cambiar el estado."); }
        });

        Button btnEliminar = new Button("Eliminar camarero");
        btnEliminar.setStyle("-fx-background-color:rgba(180,40,70,0.75);" +
            "-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;-fx-cursor:hand;");
        btnEliminar.setOnAction(e -> {
            Usuario sel = tabla.getSelectionModel().getSelectedItem();
            if (sel == null) { alerta("Selecciona un camarero de la tabla."); return; }
            if ("ADMIN".equals(sel.getRol())) { alerta("No se puede eliminar un administrador."); return; }
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("NEXERVO");
            confirm.setHeaderText("Eliminar camarero");
            confirm.setContentText("¿Eliminar a " + sel.getNombre() + "?\nEsta acción no se puede deshacer.");
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.OK) {
                    dao.eliminarUsuario(sel.getIdUsuario());
                    recargar.run();
                }
            });
        });

        javafx.scene.layout.HBox botonesAccion = new javafx.scene.layout.HBox(8,
            btnCambiarPass, btnToggleActivo, btnEliminar);

        VBox contenido = new VBox(12,
            new Label("Lista de camareros:"),
            tabla,
            lblAcciones,
            txtNuevoPass,
            botonesAccion,
            new Separator(),
            lblCrear,
            new Label("Nombre:"), txtNombre,
            new Label("Usuario:"), txtLogin,
            new Label("Contraseña:"), txtPass,
            btnCrear
        );
        contenido.setPadding(new Insets(20));
        contenido.setStyle("-fx-background-color: #1A0A3C;");
        contenido.getChildren().forEach(n -> {
            if (n instanceof Label l && !l.getStyle().contains("text-fill"))
                l.setStyle("-fx-text-fill:white;");
        });

        ScrollPane scroll = new ScrollPane(contenido);
        scroll.setFitToWidth(true);
        contenido.setStyle("-fx-background-color:#1A0A3C; -fx-background:#1A0A3C;");
        dialogo.setScene(new Scene(scroll, 500, 620));
    }

    private static void alerta(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("NEXERVO"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    // ── Navegación ────────────────────────────────────────────────

    private void abrirPrincipal(Usuario usuario) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/vista/principal.fxml"));
            Scene scene = new Scene(loader.load());
            PrincipalControlador ctrl = loader.getController();
            ctrl.inicializar(usuario);
            Stage stage = (Stage) btnEntrar.getScene().getWindow();
            stage.setTitle("NEXERVO · " + usuario.getNombre() + " [" + usuario.getRol() + "]");
            stage.setScene(scene);
            stage.setMinWidth(960); stage.setMinHeight(640);
        } catch (IOException e) {
            mostrarError("Error al cargar la aplicación.");
            e.printStackTrace();
        }
    }

    private void mostrarError(String msg) {
        lblError.setText(msg);
        lblError.setVisible(true);
    }
}
