package com.nexervo.controlador;

import com.nexervo.datos.ClienteDAO;
import com.nexervo.datos.ReservaDAO;
import com.nexervo.modelo.Cliente;
import com.nexervo.modelo.Intolerancia;
import com.nexervo.modelo.Reserva;
import com.nexervo.modelo.Usuario;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controlador de la vista de clientes.
 *
 * Gestión completa de la ficha del cliente: datos personales,
 * alergias / intolerancias y, en v3, el historial de visitas.
 *
 * El historial fue idea propia — en hostelería es muy importante saber
 * si alguien es cliente habitual y cuántas veces ha venido. Con eso
 * puedes saludarle por su nombre o sorprenderle recordando que la última
 * vez pidió un vino concreto. Pequeños detalles que marcan la diferencia.
 *
 * El gasto estimado es una aproximación (precio medio * comensales * visitas)
 * porque no tenemos TPV integrado, pero da una idea de qué clientes generan más valor.
 * La exportación del historial a PDF queda fuera del alcance de esta versión.
 */
public class ClientesControlador implements PrincipalControlador.NecesitaUsuario {

    // ── Tabla (izquierda) ─────────────────────────────────────────
    @FXML private TextField                    txtBuscar;
    @FXML private TableView<Cliente>           tabla;
    @FXML private TableColumn<Cliente,String>  colNombre;
    @FXML private TableColumn<Cliente,String>  colTelefono;
    @FXML private TableColumn<Cliente,String>  colEmail;

    // ── Formulario (derecha) ──────────────────────────────────────
    @FXML private Label    lblTituloPanel;
    @FXML private TextField txtNombreEdit;
    @FXML private TextField txtTelefonoEdit;
    @FXML private TextField txtEmailEdit;
    @FXML private TextArea  txtObsEdit;
    @FXML private VBox      vboxIntol;
    @FXML private ListView<Intolerancia> listaIntol;
    @FXML private Label     lblResumenIntol;
    @FXML private Button    btnGuardar;
    @FXML private Button    btnCancelar;
    @FXML private Button    btnEliminar;

    // ── Historial de visitas ──────────────────────────────────────
    @FXML private VBox   panelHistorial;
    @FXML private Label  lblVisitas;
    @FXML private Label  lblAusenciasCliente;
    @FXML private Label  lblGastoEstimado;
    @FXML private TableView<Reserva>           tablaHistorial;
    @FXML private TableColumn<Reserva,String>  colHistFecha;
    @FXML private TableColumn<Reserva,String>  colHistMesa;
    @FXML private TableColumn<Reserva,String>  colHistPax;
    @FXML private TableColumn<Reserva,String>  colHistOcasion;
    @FXML private TableColumn<Reserva,String>  colHistEstado;

    private final ClienteDAO            clienteDAO   = new ClienteDAO();
    private final ReservaDAO            reservaDAO   = new ReservaDAO();
    private List<Cliente>               todos        = new ArrayList<>();
    private List<Intolerancia>          catalogoIntol = new ArrayList<>();
    private final Set<Integer>          idsIntolSelec = new HashSet<>();
    private Cliente                     seleccionado;
    private boolean                     modoCreacion = false;
    private boolean                     esAdmin      = false;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Ciclo de vida ─────────────────────────────────────────────

    @Override
    public void setUsuario(Usuario u) {
        esAdmin = u != null && u.esAdmin();
        if (btnEliminar != null) {
            btnEliminar.setVisible(esAdmin);
            btnEliminar.setManaged(esAdmin);
        }
    }

    @FXML
    public void initialize() {
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colNombre.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().getNombre()));
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTelefono()));
        colEmail.setCellValueFactory(c    -> new SimpleStringProperty(
            c.getValue().getEmail() != null ? c.getValue().getEmail() : "—"));

        tabla.getSelectionModel().selectedItemProperty().addListener(
            (o, v, n) -> { if (n != null) mostrar(n); }
        );

        txtBuscar.textProperty().addListener((o, v, texto) -> filtrar(texto));

        // Filtro de escritura en el campo de teléfono: solo dígitos, máximo 9 caracteres
        txtTelefonoEdit.textProperty().addListener((o, v, n) -> {
            if (!n.matches("\\d*") || n.length() > 9) txtTelefonoEdit.setText(v);
        });

        // Tabla de historial
        configurarTablaHistorial();
        configurarIntolerancias();
        cargar();
        desactivar();
    }

    // ── Tabla historial ───────────────────────────────────────────

    private void configurarTablaHistorial() {
        if (tablaHistorial == null) return;
        tablaHistorial.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colHistFecha.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getFechaReserva().format(FMT)));
        colHistMesa.setCellValueFactory(c  -> new SimpleStringProperty(c.getValue().getNumeroMesa()));
        colHistPax.setCellValueFactory(c   -> new SimpleStringProperty(String.valueOf(c.getValue().getComensales())));
        colHistOcasion.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getOcasion()));
        colHistEstado.setCellValueFactory(c  -> new SimpleStringProperty(c.getValue().getEstadoReserva()));

        // Colorear filas según estado
        tablaHistorial.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Reserva r, boolean empty) {
                super.updateItem(r, empty);
                getStyleClass().removeAll("fila-cancelada","fila-finalizada","fila-no-presentado","fila-ocasion");
                if (!empty && r != null) {
                    switch (r.getEstadoReserva()) {
                        case "CANCELADA"  -> getStyleClass().add("fila-cancelada");
                        case "FINALIZADA" -> getStyleClass().add("fila-finalizada");
                        case "No presentado"    -> getStyleClass().add("fila-no-presentado");
                        default -> { if (r.tieneOcasionEspecial()) getStyleClass().add("fila-ocasion"); }
                    }
                }
            }
        });
    }

    private void cargarHistorial(Cliente c) {
        if (panelHistorial == null) return;
        List<Reserva> historial = reservaDAO.obtenerPorCliente(c.getIdCliente());

        // Solo cuentan las FINALIZADAS como visitas reales (las canceladas y no se presentós no)
        long visitas = historial.stream().filter(r -> "FINALIZADA".equals(r.getEstadoReserva())).count();
        long ausencias = historial.stream().filter(r -> "No presentado".equals(r.getEstadoReserva())).count();
        // Estimación de gasto: 10 € de ticket medio por comensal (precio orientativo)
        double gasto = historial.stream()
            .filter(r -> "FINALIZADA".equals(r.getEstadoReserva()))
            .mapToDouble(r -> r.getComensales() * 10.0)
            .sum();

        if (lblVisitas != null)         lblVisitas.setText(String.valueOf(visitas));
        if (lblAusenciasCliente != null)  {
            lblAusenciasCliente.setText(String.valueOf(ausencias));
            lblAusenciasCliente.setStyle(ausencias > 0
                ? "-fx-text-fill:#FF6688; -fx-font-size:22px; -fx-font-weight:bold;"
                : "-fx-text-fill:#00E676; -fx-font-size:22px; -fx-font-weight:bold;");
        }
        if (lblGastoEstimado != null)   lblGastoEstimado.setText(String.format("%.0f €", gasto));

        if (tablaHistorial != null) {
            tablaHistorial.setItems(FXCollections.observableArrayList(historial));
        }

        panelHistorial.setVisible(true);
        panelHistorial.setManaged(true);
    }

    // ── Intolerancias con patrón Set ──────────────────────────────

    private void configurarIntolerancias() {
        catalogoIntol = clienteDAO.obtenerCatalogoIntolerancias();

        ObservableList<Intolerancia> items = FXCollections.observableArrayList();
        items.add(new Intolerancia(-1, "── 14 ALÉRGENOS OFICIALES UE ──", "CABECERA"));
        catalogoIntol.stream().filter(i -> "ALERGENO_UE".equals(i.getTipo())).forEach(items::add);
        items.add(new Intolerancia(-2, "── INTOLERANCIAS COMUNES ──", "CABECERA"));
        catalogoIntol.stream().filter(i -> "INTOLERANCIA".equals(i.getTipo())).forEach(items::add);
        listaIntol.setItems(items);

        listaIntol.setCellFactory(lv -> new ListCell<Intolerancia>() {
            @Override protected void updateItem(Intolerancia item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(null); setText(null); setStyle(""); setDisable(false);
                if (empty || item == null) return;
                if ("CABECERA".equals(item.getTipo())) {
                    setText(item.getNombre());
                    setStyle("-fx-font-weight:bold;-fx-text-fill:#9090BB;-fx-font-size:11px;");
                    setDisable(true);
                } else {
                    CheckBox cb = new CheckBox(item.getNombre());
                    cb.setStyle("-fx-text-fill:white;-fx-font-size:12px;");
                    cb.setSelected(idsIntolSelec.contains(item.getIdIntolerancia()));
                    cb.setOnAction(e -> {
                        if (cb.isSelected()) idsIntolSelec.add(item.getIdIntolerancia());
                        else                 idsIntolSelec.remove(item.getIdIntolerancia());
                        actualizarResumenIntol();
                    });
                    setGraphic(cb);
                }
            }
        });
    }

    private void actualizarResumenIntol() {
        int n = idsIntolSelec.size();
        if (lblResumenIntol != null)
            lblResumenIntol.setText(n == 0 ? "Ninguna seleccionada" : n + " seleccionada(s)");
    }

    // ── Carga y filtrado ──────────────────────────────────────────

    private void cargar() {
        todos = clienteDAO.listarClientes();
        tabla.setItems(FXCollections.observableArrayList(todos));
    }

    private void filtrar(String texto) {
        if (texto == null || texto.isBlank()) {
            tabla.setItems(FXCollections.observableArrayList(todos));
            return;
        }
        String low = texto.toLowerCase();
        tabla.setItems(FXCollections.observableArrayList(
            todos.stream().filter(c ->
                c.getNombre().toLowerCase().contains(low) ||
                c.getTelefono().contains(low) ||
                (c.getEmail() != null && c.getEmail().toLowerCase().contains(low))
            ).collect(Collectors.toList())
        ));
    }

    // ── Mostrar detalle ───────────────────────────────────────────

    private void mostrar(Cliente c) {
        if (modoCreacion) return;
        seleccionado = c;
        txtNombreEdit.setText(c.getNombre());
        txtTelefonoEdit.setText(c.getTelefono());
        txtEmailEdit.setText(c.getEmail() != null ? c.getEmail() : "");
        txtObsEdit.setText(c.getObservaciones() != null ? c.getObservaciones() : "");

        List<Intolerancia> suyas = clienteDAO.obtenerIntoleranciasPorCliente(c.getIdCliente());
        idsIntolSelec.clear();
        for (Intolerancia i : suyas) idsIntolSelec.add(i.getIdIntolerancia());
        listaIntol.refresh();
        actualizarResumenIntol();

        activar();
        cargarHistorial(c);
    }

    // ── Acciones ──────────────────────────────────────────────────

    @FXML
    public void onNuevo() {
        modoCreacion = true;
        seleccionado = null;
        tabla.getSelectionModel().clearSelection();

        txtNombreEdit.clear();   txtNombreEdit.setDisable(false);
        txtTelefonoEdit.clear(); txtTelefonoEdit.setDisable(false);
        txtEmailEdit.clear();    txtEmailEdit.setDisable(false);
        txtObsEdit.clear();      txtObsEdit.setDisable(false);
        vboxIntol.setDisable(false);
        idsIntolSelec.clear();
        listaIntol.refresh();
        actualizarResumenIntol();

        if (lblTituloPanel != null) lblTituloPanel.setText("NUEVO CLIENTE");
        btnGuardar.setText("Crear cliente");
        btnGuardar.setDisable(false);
        btnCancelar.setVisible(true);
        btnCancelar.setManaged(true);
        if (btnEliminar != null) btnEliminar.setDisable(true);

        // Ocultar historial en modo creación
        if (panelHistorial != null) {
            panelHistorial.setVisible(false);
            panelHistorial.setManaged(false);
        }

        txtNombreEdit.requestFocus();
    }

    @FXML
    public void onCancelar() {
        desactivar();
        tabla.getSelectionModel().clearSelection();
    }

    @FXML
    public void onGuardar() {
        String nombre = txtNombreEdit.getText().trim();
        String tel    = txtTelefonoEdit.getText().trim();

        if (nombre.isBlank()) { alerta("El nombre completo es obligatorio."); txtNombreEdit.requestFocus(); return; }
        if (tel.isBlank() || tel.length() != 9) {
            alerta("El teléfono debe tener exactamente 9 dígitos.");
            txtTelefonoEdit.requestFocus();
            return;
        }
        String emailVal = txtEmailEdit.getText().trim();
        if (!emailVal.isBlank() && !emailVal.matches("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$")) {
            alerta("El formato del email no es válido (ejemplo: nombre@dominio.com).");
            txtEmailEdit.requestFocus();
            return;
        }

        if (modoCreacion) {
            if (clienteDAO.buscarPorTelefono(tel) != null) {
                alerta("Ya existe un cliente con el teléfono " + tel + ".\nUsa la búsqueda para encontrarlo y editarlo.");
                return;
            }
            Cliente nuevo = new Cliente(
                nombre, tel,
                txtEmailEdit.getText().isBlank() ? null : txtEmailEdit.getText().trim(),
                txtObsEdit.getText().isBlank()   ? null : txtObsEdit.getText().trim()
            );
            int id = clienteDAO.registrarCliente(nuevo);
            if (id == -1) { alerta("No se pudo crear el cliente. Revisa la conexión con la base de datos."); return; }
            nuevo.setIdCliente(id);

            for (Intolerancia intol : catalogoIntol) {
                if (idsIntolSelec.contains(intol.getIdIntolerancia()))
                    clienteDAO.vincularIntolerancia(id, intol.getIdIntolerancia());
            }

            info("Cliente creado correctamente.");
            desactivar();
            cargar();
            final int idNuevo = id;
            tabla.getItems().stream()
                .filter(c -> c.getIdCliente() == idNuevo)
                .findFirst()
                .ifPresent(c -> tabla.getSelectionModel().select(c));

        } else {
            if (seleccionado == null) return;
            seleccionado.setNombre(nombre);
            seleccionado.setTelefono(tel);
            seleccionado.setEmail(txtEmailEdit.getText().isBlank() ? null : txtEmailEdit.getText().trim());
            seleccionado.setObservaciones(txtObsEdit.getText().isBlank() ? null : txtObsEdit.getText().trim());

            boolean ok = clienteDAO.actualizarCliente(seleccionado);
            if (!ok) { alerta("No se pudo actualizar el cliente."); return; }

            for (Intolerancia intol : catalogoIntol) {
                if (idsIntolSelec.contains(intol.getIdIntolerancia()))
                    clienteDAO.vincularIntolerancia(seleccionado.getIdCliente(), intol.getIdIntolerancia());
                else
                    clienteDAO.desvincularIntolerancia(seleccionado.getIdCliente(), intol.getIdIntolerancia());
            }

            info("Cliente actualizado correctamente.");
            int idSel = seleccionado.getIdCliente();
            cargar();
            tabla.getItems().stream()
                .filter(c -> c.getIdCliente() == idSel)
                .findFirst()
                .ifPresent(c -> tabla.getSelectionModel().select(c));
        }
    }

    @FXML
    public void onEliminar() {
        if (seleccionado == null || !esAdmin) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("NEXERVO"); confirm.setHeaderText("Eliminar cliente");
        confirm.setContentText("¿Eliminar a " + seleccionado.getNombre() + "?\n" +
            "Se eliminarán también todas sus reservas. Esta acción no se puede deshacer.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                clienteDAO.eliminarCliente(seleccionado.getIdCliente());
                desactivar();
                seleccionado = null;
                cargar();
            }
        });
    }

    // ── Estado del formulario ─────────────────────────────────────

    private void activar() {
        txtNombreEdit.setDisable(false); txtTelefonoEdit.setDisable(false);
        txtEmailEdit.setDisable(false);  txtObsEdit.setDisable(false);
        vboxIntol.setDisable(false);
        btnGuardar.setDisable(false);
        if (btnEliminar != null) btnEliminar.setDisable(!esAdmin);
    }

    private void desactivar() {
        modoCreacion = false;
        txtNombreEdit.clear(); txtNombreEdit.setDisable(true);
        txtTelefonoEdit.clear(); txtTelefonoEdit.setDisable(true);
        txtEmailEdit.clear(); txtEmailEdit.setDisable(true);
        txtObsEdit.clear(); txtObsEdit.setDisable(true);
        vboxIntol.setDisable(true);
        idsIntolSelec.clear();
        if (listaIntol != null) listaIntol.refresh();
        if (lblResumenIntol != null) lblResumenIntol.setText("Ninguna seleccionada");
        btnGuardar.setDisable(true);
        btnGuardar.setText("Guardar cambios");
        btnCancelar.setVisible(false);
        btnCancelar.setManaged(false);
        if (btnEliminar != null) btnEliminar.setDisable(true);
        if (lblTituloPanel != null) lblTituloPanel.setText("DATOS DEL CLIENTE");
        if (panelHistorial != null) {
            panelHistorial.setVisible(false);
            panelHistorial.setManaged(false);
        }
    }

    // ── Diálogos ──────────────────────────────────────────────────

    private void alerta(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("NEXERVO"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("NEXERVO"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
