package com.nexervo.controlador;

import com.nexervo.datos.UsuarioDAO;
import com.nexervo.modelo.Usuario;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Optional;

/**
 * Controlador de la pantalla de Gestión de Camareros (empleados).
 * Permite crear, editar, activar/desactivar y eliminar usuarios.
 * Solo accesible para rol ADMIN.
 */
public class CamarerosControlador implements PrincipalControlador.NecesitaUsuario {

    // ── Tabla ────────────────────────────────────────────────────
    @FXML private TextField   txtBuscar;
    @FXML private ToggleButton filtroTodos;
    @FXML private ToggleButton filtroEmpleados;
    @FXML private ToggleButton filtroAdmins;
    @FXML private Label        lblContador;
    @FXML private TableView<Usuario>         tabla;
    @FXML private TableColumn<Usuario,String> colNombre;
    @FXML private TableColumn<Usuario,String> colUsuario;
    @FXML private TableColumn<Usuario,String> colRol;
    @FXML private TableColumn<Usuario,String> colActivo;

    // ── Paneles derecha ─────────────────────────────────────────
    @FXML private VBox       panelPlaceholder;
    @FXML private ScrollPane scrollDetalle;

    // ── Formulario ───────────────────────────────────────────────
    @FXML private Label         lblTituloForm;
    @FXML private Label         lblIdUsuario;
    @FXML private TextField     txtNombre;
    @FXML private TextField     txtUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private Label         lblContrasenaHint;
    @FXML private Label         lblPassInfo;
    @FXML private ComboBox<String> cmbRol;
    @FXML private VBox          panelEstado;
    @FXML private Label         lblEstadoActual;
    @FXML private Button        btnToggleActivo;
    @FXML private Label         lblMensaje;
    @FXML private Button        btnEliminar;

    // ── Estado interno ───────────────────────────────────────────
    private final UsuarioDAO dao = new UsuarioDAO();
    private ObservableList<Usuario> todos = FXCollections.observableArrayList();
    private FilteredList<Usuario>   filtrados;
    private boolean modoEdicion = false;
    private Usuario usuarioActivo; // el que está logueado

    // ── Filtro activo ─────────────────────────────────────────────
    private String filtroRol = "TODOS"; // "TODOS" | "EMPLEADO" | "ADMIN"

    // ────────────────────────────────────────────────────────────
    @FXML
    public void initialize() {
        // Columnas
        colNombre.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getNombre()));
        colUsuario.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getUsuario()));
        colRol.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(c.getValue().getRol()));
        colActivo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().isActivo() ? "Activo" : "Inactivo"));

        // Colores en la columna de estado
        colActivo.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("Activo".equals(item)
                        ? "-fx-text-fill: #00E676; -fx-font-weight: bold;"
                        : "-fx-text-fill: #FF6688; -fx-font-weight: bold;");
            }
        });

        // Política de columnas responsivas
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // ComboBox de roles
        cmbRol.setItems(FXCollections.observableArrayList("EMPLEADO", "ADMIN"));
        cmbRol.getSelectionModel().selectFirst();

        // Datos + filtrado
        filtrados = new FilteredList<>(todos, u -> true);
        tabla.setItems(filtrados);

        // Listener de búsqueda en tiempo real
        txtBuscar.textProperty().addListener((obs, ov, nv) -> actualizarFiltro());

        // Listener de selección en tabla
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
            if (nv != null) cargarEnFormulario(nv);
        });

        cargarTabla();
        mostrarPlaceholder();
    }

    @Override
    public void setUsuario(Usuario u) {
        this.usuarioActivo = u;
    }

    // ── Carga de datos ───────────────────────────────────────────
    private void cargarTabla() {
        List<Usuario> lista = dao.listarEmpleados();
        todos.setAll(lista);
        actualizarFiltro();
    }

    // ── Filtrado ──────────────────────────────────────────────────
    @FXML
    public void onFiltrar() {
        if (filtroEmpleados.isSelected())      filtroRol = "EMPLEADO";
        else if (filtroAdmins.isSelected())    filtroRol = "ADMIN";
        else                                   filtroRol = "TODOS";
        // Aseguramos estado de los toggles (simula radio group)
        filtroTodos.setSelected("TODOS".equals(filtroRol));
        filtroEmpleados.setSelected("EMPLEADO".equals(filtroRol));
        filtroAdmins.setSelected("ADMIN".equals(filtroRol));
        actualizarFiltro();
    }

    private void actualizarFiltro() {
        String texto = txtBuscar.getText().toLowerCase().trim();
        filtrados.setPredicate(u -> {
            boolean rolOk = "TODOS".equals(filtroRol) || filtroRol.equals(u.getRol());
            boolean textoOk = texto.isEmpty()
                    || u.getNombre().toLowerCase().contains(texto)
                    || u.getUsuario().toLowerCase().contains(texto);
            return rolOk && textoOk;
        });
        lblContador.setText(filtrados.size() + " resultado" + (filtrados.size() != 1 ? "s" : ""));
    }

    // ── Formulario: mostrar/ocultar paneles ──────────────────────
    private void mostrarPlaceholder() {
        panelPlaceholder.setVisible(true);
        panelPlaceholder.setManaged(true);
        scrollDetalle.setVisible(false);
        scrollDetalle.setManaged(false);
    }

    private void mostrarFormulario() {
        panelPlaceholder.setVisible(false);
        panelPlaceholder.setManaged(false);
        scrollDetalle.setVisible(true);
        scrollDetalle.setManaged(true);
    }

    // ── Nuevo camarero ────────────────────────────────────────────
    @FXML
    public void onNuevo() {
        modoEdicion = false;
        tabla.getSelectionModel().clearSelection();
        limpiarFormulario();
        lblTituloForm.setText("NUEVO CAMARERO");
        lblPassInfo.setVisible(false);
        lblPassInfo.setManaged(false);
        lblContrasenaHint.setText("Contraseña");
        panelEstado.setVisible(false);
        panelEstado.setManaged(false);
        btnEliminar.setVisible(false);
        btnEliminar.setManaged(false);
        ocultarMensaje();
        mostrarFormulario();
    }

    // ── Cargar usuario seleccionado en formulario ────────────────
    private void cargarEnFormulario(Usuario u) {
        modoEdicion = true;
        lblTituloForm.setText("EDITAR CAMARERO");
        lblIdUsuario.setText(String.valueOf(u.getIdUsuario()));
        txtNombre.setText(u.getNombre());
        txtUsuario.setText(u.getUsuario());
        txtContrasena.clear();
        cmbRol.setValue(u.getRol());

        // Contraseña: dejar en blanco = no cambiar
        lblPassInfo.setVisible(true);
        lblPassInfo.setManaged(true);
        lblContrasenaHint.setText("Nueva contraseña");

        // Estado
        panelEstado.setVisible(true);
        panelEstado.setManaged(true);
        actualizarLabelEstado(u.isActivo());

        // Eliminar solo si no es ADMIN o si no es el propio usuario logueado
        boolean puedeEliminar = (usuarioActivo == null || u.getIdUsuario() != usuarioActivo.getIdUsuario());
        btnEliminar.setVisible(puedeEliminar);
        btnEliminar.setManaged(puedeEliminar);

        ocultarMensaje();
        mostrarFormulario();
    }

    private void actualizarLabelEstado(boolean activo) {
        lblEstadoActual.setText(activo ? "Activo" : "Inactivo");
        lblEstadoActual.setStyle(activo
                ? "-fx-text-fill:#00E676; -fx-font-size:13px; -fx-font-weight:bold;"
                : "-fx-text-fill:#FF6688; -fx-font-size:13px; -fx-font-weight:bold;");
        btnToggleActivo.setText(activo ? "Desactivar" : "Activar");
        btnToggleActivo.setStyle(activo
                ? "-fx-background-color:rgba(180,40,70,0.75); -fx-text-fill:white; -fx-font-size:12px; -fx-background-radius:8; -fx-cursor:hand; -fx-padding:6 14 6 14;"
                : "-fx-background-color:rgba(0,160,90,0.6); -fx-text-fill:white; -fx-font-size:12px; -fx-background-radius:8; -fx-cursor:hand; -fx-padding:6 14 6 14;");
    }

    // ── Guardar (crear o actualizar) ──────────────────────────────
    @FXML
    public void onGuardar() {
        ocultarMensaje();
        String nombre    = txtNombre.getText().trim();
        String login     = txtUsuario.getText().trim();
        String pass      = txtContrasena.getText();
        String rol       = cmbRol.getValue();

        // Validaciones básicas
        if (nombre.isEmpty() || login.isEmpty()) {
            mostrarError("El nombre y el usuario son obligatorios.");
            return;
        }
        if (!modoEdicion && pass.isEmpty()) {
            mostrarError("La contraseña es obligatoria al crear un nuevo camarero.");
            return;
        }
        if (!modoEdicion && pass.length() < 6) {
            mostrarError("La contraseña debe tener al menos 6 caracteres.");
            return;
        }

        if (modoEdicion) {
            int id = Integer.parseInt(lblIdUsuario.getText());
            // Verificar que el login no lo usa ningún otro usuario
            if (!loginEstaDisponible(login, id)) {
                mostrarError("El nombre de usuario ya está en uso por otra cuenta.");
                return;
            }
            // Si se introduce nueva contraseña, debe tener al menos 6 caracteres
            if (!pass.isEmpty() && pass.length() < 6) {
                mostrarError("La contraseña debe tener al menos 6 caracteres.");
                return;
            }
            boolean ok = dao.actualizarUsuario(id, nombre, login, rol);
            if (!pass.isEmpty()) dao.cambiarContrasena(id, pass);
            if (ok) {
                mostrarExito("Camarero actualizado correctamente.");
                cargarTabla();
                // Refrescar el item en la tabla
                tabla.getItems().stream()
                        .filter(u -> u.getIdUsuario() == id)
                        .findFirst()
                        .ifPresent(this::cargarEnFormulario);
                // Re-seleccionar para refrescar vista
                reseleccionarPorId(id);
            } else {
                mostrarError("Error al guardar los cambios. Comprueba la consola.");
            }
        } else {
            // Crear nuevo
            if (dao.existeUsuario(login)) {
                mostrarError("El nombre de usuario «" + login + "» ya existe.");
                return;
            }
            Usuario nuevo = new Usuario(nombre, login, pass, rol);
            boolean ok = dao.crearUsuario(nuevo);
            if (ok) {
                mostrarExito("Camarero creado correctamente.");
                cargarTabla();
                mostrarPlaceholder();
            } else {
                mostrarError("Error al crear el camarero. Comprueba la consola.");
            }
        }
    }

    // ── Activar / Desactivar ──────────────────────────────────────
    @FXML
    public void onToggleActivo() {
        Usuario sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        // Proteger: no desactivarse a uno mismo
        if (usuarioActivo != null && sel.getIdUsuario() == usuarioActivo.getIdUsuario()) {
            mostrarError("No puedes desactivar tu propia cuenta.");
            return;
        }

        boolean nuevoEstado = !sel.isActivo();
        boolean ok = dao.cambiarEstadoUsuario(sel.getIdUsuario(), nuevoEstado);
        if (ok) {
            cargarTabla();
            reseleccionarPorId(sel.getIdUsuario());
        } else {
            mostrarError("Error al cambiar el estado. Comprueba la consola.");
        }
    }

    // ── Eliminar ──────────────────────────────────────────────────
    @FXML
    public void onEliminar() {
        Usuario sel = tabla.getSelectionModel().getSelectedItem();
        if (sel == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar eliminación");
        confirm.setHeaderText("¿Eliminar al camarero «" + sel.getNombre() + "»?");
        confirm.setContentText("Esta acción no se puede deshacer.");
        // Estilar el diálogo con el tema oscuro
        confirm.getDialogPane().getStylesheets().add(
                getClass().getResource("/Estilo.css").toExternalForm());

        Optional<ButtonType> resultado = confirm.showAndWait();
        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            boolean ok = dao.eliminarUsuario(sel.getIdUsuario());
            if (ok) {
                cargarTabla();
                mostrarPlaceholder();
            } else {
                mostrarError("No se puede eliminar (puede tener reservas asociadas o es un ADMIN).");
            }
        }
    }

    // ── Cancelar / limpiar formulario ─────────────────────────────
    @FXML
    public void onCancelar() {
        tabla.getSelectionModel().clearSelection();
        limpiarFormulario();
        mostrarPlaceholder();
    }

    // ── Utilidades ────────────────────────────────────────────────
    private void limpiarFormulario() {
        txtNombre.clear();
        txtUsuario.clear();
        txtContrasena.clear();
        cmbRol.getSelectionModel().selectFirst();
        lblIdUsuario.setText("");
    }

    private void mostrarError(String msg) {
        lblMensaje.setText(msg);
        lblMensaje.setStyle("-fx-text-fill:#FF6688; -fx-font-size:12px; -fx-font-weight:bold;");
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
    }

    private void mostrarExito(String msg) {
        lblMensaje.setText(msg);
        lblMensaje.setStyle("-fx-text-fill:#00E676; -fx-font-size:12px; -fx-font-weight:bold;");
        lblMensaje.setVisible(true);
        lblMensaje.setManaged(true);
    }

    private void ocultarMensaje() {
        lblMensaje.setVisible(false);
        lblMensaje.setManaged(false);
    }

    /**
     * Devuelve true si el login está libre para el usuario con idPropio
     * (no lo usa ningún OTRO usuario distinto).
     */
    private boolean loginEstaDisponible(String login, int idPropio) {
        return todos.stream().noneMatch(u -> u.getUsuario().equalsIgnoreCase(login)
                && u.getIdUsuario() != idPropio);
    }

    /** Reselecciona un usuario en la tabla por ID y recarga el formulario. */
    private void reseleccionarPorId(int id) {
        for (int i = 0; i < tabla.getItems().size(); i++) {
            if (tabla.getItems().get(i).getIdUsuario() == id) {
                tabla.getSelectionModel().select(i);
                break;
            }
        }
    }
}
