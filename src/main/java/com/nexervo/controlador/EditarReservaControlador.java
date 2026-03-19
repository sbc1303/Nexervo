package com.nexervo.controlador;

import datos.ClienteDAO;
import datos.ReservaDAO;
import modelo.Cliente;
import modelo.Reserva;
import modelo.ReservaView;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador para editar una reserva existente.
 */
public class EditarReservaControlador {

    @FXML private TextField  txtNombre, txtApellidos, txtTelefono, txtEmail;
    @FXML private Spinner<Integer> spinnerComensales;
    @FXML private DatePicker fechaReserva;
    @FXML private ComboBox<String> comboHora;
    @FXML private ListView<String> listaAlergias;
    @FXML private TextArea  txtObservaciones;
    @FXML private Label     lblMesa, lblSeleccionadas;

    private ReservaDAO reservaDAO;
    private ClienteDAO clienteDAO;
    private ReservaView reservaActual;

    private final List<String> horasComida = List.of("13:00","13:30","14:00","14:30","15:00");
    private final List<String> horasCena   = List.of("20:30","21:00","21:30","22:00","22:30");

    @FXML
    public void initialize() {
        reservaDAO = new ReservaDAO();
        clienteDAO = new ClienteDAO();

        txtTelefono.textProperty().addListener((o, old, v) -> {
            if (!v.matches("\\d*") || v.length() > 9) txtTelefono.setText(old);
        });

        SpinnerValueFactory<Integer> vf = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1);
        spinnerComensales.setValueFactory(vf);
        spinnerComensales.setEditable(true);

        configurarListaAlergias();
        configurarCalendario();
    }

    private void configurarCalendario() {
        fechaReserva.setDayCellFactory(picker -> new DateCell() {
            @Override public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #2a2a3b;");
                }
            }
        });
        fechaReserva.valueProperty().addListener((o, old, v) -> actualizarComboHoras(v));
    }

    private void configurarListaAlergias() {
        List<String> todas = clienteDAO.obtenerIntolerancias();
        listaAlergias.setItems(FXCollections.observableArrayList(todas));
        listaAlergias.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        listaAlergias.getSelectionModel().getSelectedItems().addListener(
            (javafx.collections.ListChangeListener<String>) c -> {
                int n = listaAlergias.getSelectionModel().getSelectedItems().size();
                if (lblSeleccionadas != null)
                    lblSeleccionadas.setText(n == 0 ? "Ninguna" : n + " seleccionada(s)");
            });
    }

    private void actualizarComboHoras(LocalDate fecha) {
        if (fecha == null) return;
        LocalTime ahora = LocalTime.now().plusMinutes(15);
        javafx.collections.ObservableList<String> items = FXCollections.observableArrayList();
        List<String> comida = horasComida.stream()
            .filter(h -> !fecha.equals(LocalDate.now()) || LocalTime.parse(h).isAfter(ahora))
            .collect(Collectors.toList());
        List<String> cena = horasCena.stream()
            .filter(h -> !fecha.equals(LocalDate.now()) || LocalTime.parse(h).isAfter(ahora))
            .collect(Collectors.toList());
        if (!comida.isEmpty()) { items.add("--- TURNO COMIDA ---"); items.addAll(comida); }
        if (!cena.isEmpty())   { items.add("--- TURNO CENA ---");   items.addAll(cena); }
        comboHora.setItems(items);
    }

    // ----------------------------------------------------------------
    // Carga los datos de la reserva seleccionada en el formulario
    // ----------------------------------------------------------------
    public void cargarReserva(ReservaView rv) {
        this.reservaActual = rv;

        // Separar nombre/apellidos (primer espacio = apellidos)
        String nombre = rv.getNombre();
        int espacio = nombre.indexOf(" ");
        if (espacio != -1) {
            txtNombre.setText(nombre.substring(0, espacio));
            txtApellidos.setText(nombre.substring(espacio + 1));
        } else {
            txtNombre.setText(nombre);
        }

        txtTelefono.setText(rv.getTelefono());
        txtEmail.setText(rv.getEmail());
        txtObservaciones.setText(rv.getNotas());
        lblMesa.setText("Mesa actual: " + rv.getMesa() + " (no modificable en esta versión)");

        // Fecha y hora
        fechaReserva.setValue(LocalDate.parse(rv.getFecha()));
        actualizarComboHoras(LocalDate.parse(rv.getFecha()));
        comboHora.setValue(rv.getTurno());

        // Comensales
        try {
            spinnerComensales.getValueFactory().setValue(Integer.parseInt(rv.getPersonas()));
        } catch (Exception e) {
            spinnerComensales.getValueFactory().setValue(1);
        }

        // Intolerancias del cliente
        List<String> intol = clienteDAO.obtenerIntoleranciasDeCliente(rv.getIdCliente());
        for (String item : listaAlergias.getItems()) {
            if (intol.contains(item)) {
                listaAlergias.getSelectionModel().select(item);
            }
        }
    }

    // ----------------------------------------------------------------
    @FXML
    public void onGuardarClick() {
        if (txtNombre.getText().isBlank() || txtApellidos.getText().isBlank()
                || txtTelefono.getText().isBlank()
                || fechaReserva.getValue() == null
                || comboHora.getValue() == null || comboHora.getValue().startsWith("---")) {
            alerta(Alert.AlertType.WARNING, "Rellena todos los campos obligatorios.");
            return;
        }

        // Actualizar cliente
        Cliente c = clienteDAO.obtenerClientePorId(reservaActual.getIdCliente());
        if (c != null) {
            c.setNombre(txtNombre.getText().trim() + " " + txtApellidos.getText().trim());
            c.setTelefono(txtTelefono.getText().trim());
            c.setEmail(txtEmail.getText().trim());
            List<String> alergias = new ArrayList<>(listaAlergias.getSelectionModel().getSelectedItems());
            String alStr = alergias.isEmpty() ? "Ninguna" : String.join(", ", alergias);
            c.setObservaciones("Alergia: " + alStr + " | Notas: " + txtObservaciones.getText().trim());
            clienteDAO.actualizarCliente(c);
            clienteDAO.guardarIntoleranciasCliente(c.getIdCliente(), alergias);
        }

        // Actualizar reserva
        Reserva r = reservaDAO.obtenerReservaPorId(reservaActual.getIdReserva());
        if (r != null) {
            r.setFechaReserva(fechaReserva.getValue());
            r.setHoraReserva(LocalTime.parse(comboHora.getValue()));
            r.setComensales(spinnerComensales.getValue());
            reservaDAO.actualizarReserva(r);
        }

        alerta(Alert.AlertType.INFORMATION, "Reserva actualizada correctamente.");
        ((Stage) txtNombre.getScene().getWindow()).close();
    }

    @FXML
    public void onCancelarClick() {
        ((Stage) txtNombre.getScene().getWindow()).close();
    }

    private void alerta(Alert.AlertType tipo, String msg) {
        Alert a = new Alert(tipo);
        a.setTitle("NEXERVO"); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}
