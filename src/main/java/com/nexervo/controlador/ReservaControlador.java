package com.nexervo.controlador;

import datos.ClienteDAO;
import datos.ReservaDAO;
import modelo.Cliente;
import modelo.Intolerancia;
import modelo.Reserva;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador de la vista principal de gestión de reservas.
 * Vinculado a gestiondereservas.fxml mediante @FXML.
 */
public class ReservaControlador {

    // ── Componentes FXML ─────────────────────────────────────────
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellidos;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;
    @FXML private TextField txtMesaSeleccionada;
    @FXML private Spinner<Integer> spinnerComensales;
    @FXML private DatePicker fechaReserva;
    @FXML private javafx.scene.control.ComboBox<String> comboHora;
    @FXML private ListView<String> listaAlergias;
    @FXML private Label lblSeleccionadas;
    @FXML private TextArea txtObservaciones;
    @FXML private GridPane gridMapa;

    // ── DAOs ─────────────────────────────────────────────────────
    private ClienteDAO clienteDAO;
    private ReservaDAO reservaDAO;

    // ── Estado interno ───────────────────────────────────────────
    private int idMesaSeleccionada = -1;

    private static final List<String> HORAS_COMIDA =
            List.of("13:00", "13:30", "14:00", "14:30", "15:00");

    private static final List<String> HORAS_CENA =
            List.of("20:30", "21:00", "21:30", "22:00", "22:30");

    // ════════════════════════════════════════════════════════════
    // INICIALIZACIÓN
    // ════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        clienteDAO = new ClienteDAO();
        reservaDAO = new ReservaDAO();

        configurarTelefono();
        configurarSpinner();
        configurarCalendario();
        configurarListaAlergias();
        configurarMapa();

        fechaReserva.valueProperty().addListener((o, oldValue, newValue) -> {
            actualizarComboHoras(newValue);
            actualizarSala();
        });

        comboHora.valueProperty().addListener((o, oldValue, newValue) -> actualizarSala());
    }

    // ════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE COMPONENTES
    // ════════════════════════════════════════════════════════════

    private void configurarTelefono() {
        txtTelefono.textProperty().addListener((o, oldValue, newValue) -> {
            if (!newValue.matches("\\d*") || newValue.length() > 9) {
                txtTelefono.setText(oldValue);
            }
        });
    }

    private void configurarSpinner() {
        SpinnerValueFactory<Integer> factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 20, 0);

        factory.setConverter(new StringConverter<>() {
            @Override
            public String toString(Integer value) {
                return (value == null || value == 0) ? "" : value.toString();
            }

            @Override
            public Integer fromString(String s) {
                if (s == null || s.trim().isEmpty()) {
                    return 0;
                }
                try {
                    return Integer.parseInt(s.trim());
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        });

        spinnerComensales.setValueFactory(factory);
        spinnerComensales.setEditable(true);
    }

    private void configurarCalendario() {
        fechaReserva.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (!empty && date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #2a2a3b;");
                }
            }
        });
    }

    private void configurarListaAlergias() {
        List<Intolerancia> catalogo = clienteDAO.obtenerCatalogoIntolerancias();
        ObservableList<String> items = FXCollections.observableArrayList();

        items.add("NINGUNA");

        catalogo.stream()
                .filter(i -> "ALERGENO_UE".equalsIgnoreCase(i.getTipo()) || "INTOLERANCIA".equalsIgnoreCase(i.getTipo()))
                .forEach(i -> items.add(i.getNombre()));

        listaAlergias.setItems(items);
        listaAlergias.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        listaAlergias.getSelectionModel().getSelectedItems().addListener(
                (ListChangeListener<String>) change -> actualizarTextoAlergiasSeleccionadas()
        );

        lblSeleccionadas.setText("Ninguna seleccionada");
    }

    private void actualizarTextoAlergiasSeleccionadas() {
        ObservableList<String> seleccionadas = listaAlergias.getSelectionModel().getSelectedItems();

        if (seleccionadas == null || seleccionadas.isEmpty()) {
            lblSeleccionadas.setText("Ninguna seleccionada");
            return;
        }

        if (seleccionadas.contains("NINGUNA")) {
            listaAlergias.getSelectionModel().clearSelection();
            int indexNinguna = listaAlergias.getItems().indexOf("NINGUNA");
            if (indexNinguna >= 0) {
                listaAlergias.getSelectionModel().select(indexNinguna);
            }
            lblSeleccionadas.setText("Ninguna seleccionada");
            return;
        }

        String texto = seleccionadas.stream().collect(Collectors.joining(", "));
        lblSeleccionadas.setText(texto);
    }

    private void configurarMapa() {
        for (Node node : gridMapa.getChildren()) {
            if (node instanceof Button btn) {
                btn.setOnAction(e -> seleccionarMesa(btn));
            }
        }
    }

    private void seleccionarMesa(Button btn) {
        for (Node node : gridMapa.getChildren()) {
            if (node instanceof Button b) {
                b.getStyleClass().remove("mesa-reservada");
            }
        }

        btn.getStyleClass().add("mesa-reservada");
        txtMesaSeleccionada.setText(btn.getText());
        idMesaSeleccionada = reservaDAO.resolverIdMesa(btn.getText());
    }

    // ════════════════════════════════════════════════════════════
    // LÓGICA DE SALA
    // ════════════════════════════════════════════════════════════

    private void actualizarComboHoras(LocalDate fecha) {
        if (fecha == null) {
            comboHora.getItems().clear();
            comboHora.setValue(null);
            return;
        }

        LocalTime margen = LocalTime.now().plusMinutes(15);
        ObservableList<String> items = FXCollections.observableArrayList();

        List<String> comida = HORAS_COMIDA.stream()
                .filter(h -> !fecha.equals(LocalDate.now()) || LocalTime.parse(h).isAfter(margen))
                .collect(Collectors.toList());

        List<String> cena = HORAS_CENA.stream()
                .filter(h -> !fecha.equals(LocalDate.now()) || LocalTime.parse(h).isAfter(margen))
                .collect(Collectors.toList());

        if (!comida.isEmpty()) {
            items.add("--- TURNO COMIDA ---");
            items.addAll(comida);
        }

        if (!cena.isEmpty()) {
            items.add("--- TURNO CENA ---");
            items.addAll(cena);
        }

        comboHora.setItems(items);
        comboHora.setValue(null);
    }

    private void actualizarSala() {
        boolean datosCompletos = fechaReserva.getValue() != null
                && comboHora.getValue() != null
                && !comboHora.getValue().startsWith("---");

        gridMapa.setVisible(datosCompletos);

        if (!datosCompletos) {
            return;
        }

        List<String> ocupadas = reservaDAO.obtenerMesasOcupadas(
                fechaReserva.getValue(),
                comboHora.getValue()
        );

        for (Node node : gridMapa.getChildren()) {
            if (node instanceof Button btn) {
                btn.getStyleClass().removeAll("mesa-libre", "mesa-ocupada", "mesa-reservada");
                btn.setDisable(false);
                btn.setOpacity(1.0);

                if (ocupadas.contains(btn.getText())) {
                    btn.getStyleClass().add("mesa-ocupada");
                    btn.setDisable(true);
                    btn.setOpacity(0.5);

                    if (btn.getText().equals(txtMesaSeleccionada.getText())) {
                        txtMesaSeleccionada.clear();
                        idMesaSeleccionada = -1;
                    }
                } else {
                    btn.getStyleClass().add("mesa-libre");
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    // ACCIONES DE BOTONES
    // ════════════════════════════════════════════════════════════

    @FXML
    private void onVerReservas(ActionEvent event) {
        // Ya estás en la pantalla de reservas.
        mostrarAlerta("Información", "Ya estás en la vista de reservas.");
    }

    @FXML
    private void onVerClientes(ActionEvent event) {
        // Método temporal para evitar el error del FXML.
        // Luego se puede cambiar para abrir listado_clientes.fxml.
        mostrarAlerta("Información", "Navegación a clientes pendiente de implementar.");
    }

    @FXML
    private void onConfirmarClick(ActionEvent event) {
        if (!validarFormulario()) {
            return;
        }

        Cliente cliente = clienteDAO.buscarPorTelefono(txtTelefono.getText());

        if (cliente == null) {
            String nombreCompleto = (txtNombre.getText().trim() + " " + txtApellidos.getText().trim()).trim();

            cliente = new Cliente(
                    nombreCompleto,
                    txtTelefono.getText().trim(),
                    txtEmail.getText().isBlank() ? null : txtEmail.getText().trim(),
                    null
            );

            int idNuevo = clienteDAO.registrarCliente(cliente);
            if (idNuevo == -1) {
                mostrarAlerta("Error", "No se pudo registrar el cliente. Comprueba la conexión.");
                return;
            }
            cliente.setIdCliente(idNuevo);
        }

        List<String> seleccionadas = listaAlergias.getSelectionModel().getSelectedItems();

        String textoAlergias = null;
        if (seleccionadas != null && !seleccionadas.isEmpty() && !seleccionadas.contains("NINGUNA")) {
            textoAlergias = seleccionadas.stream().collect(Collectors.joining(", "));
        }

        int comensales = spinnerComensales.getValue() == null || spinnerComensales.getValue() == 0
                ? 1
                : spinnerComensales.getValue();

        String observacionesFinales = "";
        if (textoAlergias != null && !textoAlergias.isBlank()) {
            observacionesFinales += "Alergias/Intolerancias: " + textoAlergias + ". ";
        }
        if (txtObservaciones.getText() != null && !txtObservaciones.getText().isBlank()) {
            observacionesFinales += txtObservaciones.getText().trim();
        }

        Reserva reserva = new Reserva(
                cliente.getIdCliente(),
                idMesaSeleccionada,
                fechaReserva.getValue(),
                LocalTime.parse(comboHora.getValue()),
                comensales,
                observacionesFinales.isBlank() ? null : observacionesFinales
        );

        int idReserva = reservaDAO.crearReserva(reserva);
        if (idReserva == -1) {
            mostrarAlerta("Error", "No se pudo guardar la reserva. Comprueba la conexión.");
            return;
        }

        mostrarAlerta("Reserva confirmada",
                "Reserva registrada correctamente para " + cliente.getNombre() + ".");

        onLimpiarClick(null);
        actualizarSala();
    }

    @FXML
    private void onLimpiarClick(ActionEvent event) {
        txtNombre.clear();
        txtApellidos.clear();
        txtTelefono.clear();
        txtEmail.clear();
        txtMesaSeleccionada.clear();
        txtObservaciones.clear();

        fechaReserva.setValue(null);
        comboHora.setValue(null);
        comboHora.getItems().clear();

        if (listaAlergias != null) {
            listaAlergias.getSelectionModel().clearSelection();
        }

        lblSeleccionadas.setText("Ninguna seleccionada");

        if (spinnerComensales.getValueFactory() != null) {
            spinnerComensales.getValueFactory().setValue(0);
        }

        gridMapa.setVisible(false);
        idMesaSeleccionada = -1;

        for (Node node : gridMapa.getChildren()) {
            if (node instanceof Button btn) {
                btn.getStyleClass().removeAll("mesa-libre", "mesa-ocupada", "mesa-reservada");
                btn.setDisable(false);
                btn.setOpacity(1.0);
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    // VALIDACIONES Y UTILIDADES
    // ════════════════════════════════════════════════════════════

    private boolean validarFormulario() {
        if (txtNombre.getText() == null || txtNombre.getText().isBlank()) {
            mostrarAlerta("Campo requerido", "Introduce el nombre del cliente.");
            return false;
        }

        if (txtApellidos.getText() == null || txtApellidos.getText().isBlank()) {
            mostrarAlerta("Campo requerido", "Introduce los apellidos del cliente.");
            return false;
        }

        if (txtTelefono.getText() == null || txtTelefono.getText().isBlank()) {
            mostrarAlerta("Campo requerido", "Introduce el teléfono del cliente.");
            return false;
        }

        if (fechaReserva.getValue() == null) {
            mostrarAlerta("Campo requerido", "Selecciona una fecha para la reserva.");
            return false;
        }

        if (comboHora.getValue() == null || comboHora.getValue().startsWith("---")) {
            mostrarAlerta("Campo requerido", "Selecciona un turno y hora.");
            return false;
        }

        if (txtMesaSeleccionada.getText() == null || txtMesaSeleccionada.getText().isBlank() || idMesaSeleccionada == -1) {
            mostrarAlerta("Mesa no seleccionada", "Selecciona una mesa del mapa.");
            return false;
        }

        return true;
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("NEXERVO");
        alert.setHeaderText(titulo);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}