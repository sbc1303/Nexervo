package com.nexervo.controlador;

import com.nexervo.datos.ClienteDAO;
import com.nexervo.datos.MesaDAO;
import com.nexervo.datos.PagoDAO;
import com.nexervo.datos.ReservaDAO;
import com.nexervo.modelo.Cliente;
import com.nexervo.modelo.Intolerancia;
import com.nexervo.modelo.Mesa;
import com.nexervo.modelo.Reserva;
import com.nexervo.modelo.Usuario;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controlador del formulario de nueva reserva.
 *
 * Este formulario lo puede usar tanto el administrador como el empleado.
 * La funcionalidad que más me gustó añadir fue el autorelleno por teléfono:
 * cuando escribes el número de un cliente que ya existe, se cargan solos
 * sus datos y sus alergias. En recepción eso ahorra mucho tiempo y evita
 * errores al registrar datos de clientes habituales.
 *
 * El mapa de mesas también fue complicado de hacer pero quedó muy visual.
 */
public class ReservaControlador implements PrincipalControlador.NecesitaUsuario {

    @FXML private TextField        txtNombre;
    @FXML private TextField        txtApellidos;
    @FXML private TextField        txtTelefono;
    @FXML private TextField        txtEmail;
    @FXML private Spinner<Integer> spinnerComensales;
    @FXML private DatePicker       fechaReserva;
    @FXML private ComboBox<String> comboHora;
    @FXML private TextField        txtMesaSeleccionada;
    @FXML private Label            lblInfoMesa;
    @FXML private ListView<Intolerancia> listaAlergias;
    @FXML private Label            lblAlergias;
    @FXML private ComboBox<String> comboOcasion;
    @FXML private TextArea         txtPeticiones;
    @FXML private TextArea         txtObservaciones;
    @FXML private FlowPane         flowMapa;
    @FXML private Label            lblEstadoMapa;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final MesaDAO    mesaDAO    = new MesaDAO();
    private final PagoDAO    pagoDAO    = new PagoDAO();

    private int idMesaSeleccionada = -1;
    private Usuario usuarioActivo;
    private List<Intolerancia> catalogoIntolerancias = new ArrayList<>();
    // IDs seleccionados — Set para sobrevivir al reciclaje de celdas de ListView
    private final Set<Integer> idsIntolSelec = new HashSet<>();

    private static final List<String> HORAS_COMIDA = List.of("13:00","13:30","14:00","14:30","15:00");
    private static final List<String> HORAS_CENA   = List.of("20:30","21:00","21:30","22:00","22:30");
    private static final BigDecimal   IMPORTE_PAX  = new BigDecimal("10.00");

    @Override public void setUsuario(Usuario u) { this.usuarioActivo = u; }

    @FXML
    public void initialize() {
        configurarTelefono();
        configurarSpinner();
        configurarCalendario();
        configurarIntolerancias();
        configurarOcasion();
        cargarMapa();
        fechaReserva.valueProperty().addListener((o,v,n) -> { actualizarHoras(n); actualizarMapa(); });
        comboHora.valueProperty().addListener((o,v,n) -> actualizarMapa());
        flowMapa.setVisible(false);
        lblEstadoMapa.setText("Selecciona fecha y hora para ver disponibilidad");
    }

    private void configurarOcasion() {
        if (comboOcasion != null) {
            comboOcasion.setItems(FXCollections.observableArrayList(Reserva.OCASIONES));
            comboOcasion.setValue("Ninguna");
        }
    }

    private void configurarTelefono() {
        txtTelefono.textProperty().addListener((o,v,n) -> {
            if (!n.matches("\\d*") || n.length() > 9) txtTelefono.setText(v);
        });
        txtTelefono.focusedProperty().addListener((o,v,focused) -> {
            if (!focused && txtTelefono.getText().length() >= 9) autorellenar();
        });
    }

    private void autorellenar() {
        Cliente c = clienteDAO.buscarPorTelefono(txtTelefono.getText());
        if (c == null) return;
        String[] p = c.getNombre().split(" ", 2);
        txtNombre.setText(p[0]);
        txtApellidos.setText(p.length > 1 ? p[1] : "");
        if (c.getEmail() != null) txtEmail.setText(c.getEmail());
        List<Intolerancia> suyas = clienteDAO.obtenerIntoleranciasPorCliente(c.getIdCliente());
        idsIntolSelec.clear();
        for (Intolerancia intol : suyas) idsIntolSelec.add(intol.getIdIntolerancia());
        listaAlergias.refresh(); // fuerza redibujado con el nuevo Set
        actualizarResumenAlergias();
        lblEstadoMapa.setText(suyas.isEmpty()
            ? "Cliente encontrado: " + c.getNombre()
            : "Cliente recurrente: " + c.getNombre() + " — " + suyas.size() + " intolerancia(s) marcada(s)");
    }

    private void configurarSpinner() {
        var f = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1);
        f.setConverter(new StringConverter<>() {
            @Override public String toString(Integer v)   { return v == null ? "1" : v.toString(); }
            @Override public Integer fromString(String s) {
                try { return Math.max(1, Integer.parseInt(s.trim())); } catch (NumberFormatException e) { return 1; }
            }
        });
        spinnerComensales.setValueFactory(f);
        spinnerComensales.setEditable(true);
    }

    private void configurarCalendario() {
        fechaReserva.setDayCellFactory(p -> new DateCell() {
            @Override public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setDisable(d.isBefore(LocalDate.now()));
            }
        });
    }

    private void configurarIntolerancias() {
        catalogoIntolerancias = clienteDAO.obtenerCatalogoIntolerancias();

        ObservableList<Intolerancia> items = FXCollections.observableArrayList();
        items.add(new Intolerancia(-1, "── 14 ALÉRGENOS OFICIALES UE ──", "CABECERA"));
        catalogoIntolerancias.stream().filter(i -> "ALERGENO_UE".equals(i.getTipo())).forEach(items::add);
        items.add(new Intolerancia(-2, "── INTOLERANCIAS COMUNES ──", "CABECERA"));
        catalogoIntolerancias.stream().filter(i -> "INTOLERANCIA".equals(i.getTipo())).forEach(items::add);
        listaAlergias.setItems(items);

        listaAlergias.setCellFactory(lv -> new ListCell<Intolerancia>() {
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
                        if (cb.isSelected()) { idsIntolSelec.add(item.getIdIntolerancia()); }
                        else                 { idsIntolSelec.remove(item.getIdIntolerancia()); }
                        actualizarResumenAlergias();
                    });
                    setGraphic(cb);
                }
            }
        });
    }

    private void cargarMapa() {
        List<Mesa> mesas = mesaDAO.listarMesas();
        flowMapa.getChildren().clear();
        for (Mesa m : mesas) {
            Button btn = new Button(m.getNumeroMesa());
            btn.setPrefWidth(90); btn.setPrefHeight(60);
            btn.getStyleClass().add("mesa-base");
            Tooltip.install(btn, new Tooltip("Capacidad: " + m.getCapacidad() + " pax\n" + m.getDescripcion()));
            btn.setOnAction(e -> seleccionarMesa(btn, m));
            flowMapa.getChildren().add(btn);
        }
    }

    private void seleccionarMesa(Button btn, Mesa mesa) {
        flowMapa.getChildren().forEach(n -> { if (n instanceof Button b) b.getStyleClass().remove("mesa-seleccionada"); });
        btn.getStyleClass().add("mesa-seleccionada");
        txtMesaSeleccionada.setText(mesa.getNumeroMesa());
        idMesaSeleccionada = mesa.getIdMesa();
        lblInfoMesa.setText(mesa.getNumeroMesa() + " · " + mesa.getCapacidad() + " pax · " + mesa.getDescripcion());
    }

    private void actualizarHoras(LocalDate fecha) {
        if (fecha == null) return;
        // Si es hoy, no mostrar turnos que ya han pasado (con 15 min de margen)
        // Si es otro día, mostrar todos los turnos disponibles
        LocalTime margen = LocalTime.now().plusMinutes(15);
        ObservableList<String> items = FXCollections.observableArrayList();
        List<String> comida = HORAS_COMIDA.stream().filter(h -> !fecha.equals(LocalDate.now()) || LocalTime.parse(h).isAfter(margen)).collect(Collectors.toList());
        List<String> cena   = HORAS_CENA.stream().filter(h -> !fecha.equals(LocalDate.now()) || LocalTime.parse(h).isAfter(margen)).collect(Collectors.toList());
        if (!comida.isEmpty()) { items.add("── TURNO COMIDA ──"); items.addAll(comida); }
        if (!cena.isEmpty())   { items.add("── TURNO CENA ──");   items.addAll(cena); }
        comboHora.setItems(items);
    }

    private void actualizarMapa() {
        boolean ok = fechaReserva.getValue() != null && comboHora.getValue() != null && !comboHora.getValue().startsWith("──");
        flowMapa.setVisible(ok);
        if (!ok) { lblEstadoMapa.setText("Selecciona fecha y hora para ver disponibilidad"); return; }
        lblEstadoMapa.setText("");
        List<String> ocupadas = reservaDAO.obtenerMesasOcupadas(fechaReserva.getValue(), comboHora.getValue());
        flowMapa.getChildren().forEach(n -> {
            if (n instanceof Button btn) {
                btn.getStyleClass().removeAll("mesa-libre","mesa-ocupada","mesa-seleccionada");
                btn.setDisable(false); btn.setOpacity(1.0);
                if (ocupadas.contains(btn.getText())) { btn.getStyleClass().add("mesa-ocupada"); btn.setDisable(true); btn.setOpacity(0.45); }
                else btn.getStyleClass().add("mesa-libre");
            }
        });
    }

    private void actualizarResumenAlergias() {
        int n = idsIntolSelec.size();
        lblAlergias.setText(n == 0 ? "Ninguna seleccionada" : n + " seleccionada(s)");
    }

    @FXML
    public void onConfirmar() {
        if (!validar()) return;
        Cliente cliente = clienteDAO.buscarPorTelefono(txtTelefono.getText());
        if (cliente == null) {
            cliente = new Cliente(txtNombre.getText().trim() + " " + txtApellidos.getText().trim(),
                txtTelefono.getText(), txtEmail.getText().isBlank() ? null : txtEmail.getText().trim(), null);
            int id = clienteDAO.registrarCliente(cliente);
            if (id == -1) { alerta(Alert.AlertType.ERROR, "No se pudo registrar el cliente."); return; }
            cliente.setIdCliente(id);
        }
        final int idCli = cliente.getIdCliente();
        // Sincronizar intolerancias desde el Set central
        for (Intolerancia intol : catalogoIntolerancias) {
            if (idsIntolSelec.contains(intol.getIdIntolerancia()))
                clienteDAO.vincularIntolerancia(idCli, intol.getIdIntolerancia());
            else
                clienteDAO.desvincularIntolerancia(idCli, intol.getIdIntolerancia());
        }
        // Validar solapamiento antes de insertar
        LocalTime horaReserva = LocalTime.parse(comboHora.getValue());
        if (reservaDAO.hayConflicto(idMesaSeleccionada, fechaReserva.getValue(), horaReserva, -1)) {
            alerta(Alert.AlertType.WARNING,
                "Esa mesa ya está reservada para esa fecha y hora.\n" +
                "Elige otra mesa o cambia el turno.");
            return;
        }

        String ocasion   = (comboOcasion != null && comboOcasion.getValue() != null)
                           ? comboOcasion.getValue() : "Ninguna";
        String peticion  = (txtPeticiones != null) ? txtPeticiones.getText().trim() : "";
        Reserva r = new Reserva(idCli, idMesaSeleccionada, fechaReserva.getValue(),
            horaReserva, spinnerComensales.getValue(),
            ocasion, peticion, txtObservaciones.getText().trim());
        int idR = reservaDAO.crearReserva(r);
        if (idR == -1) { alerta(Alert.AlertType.ERROR, "No se pudo guardar la reserva."); return; }
        BigDecimal importe = IMPORTE_PAX.multiply(new BigDecimal(spinnerComensales.getValue()));
        pagoDAO.crearPreautorizacion(idR, importe);
        alerta(Alert.AlertType.INFORMATION, "Reserva confirmada para " + cliente.getNombre() + ".\nMesa " + txtMesaSeleccionada.getText() + "  ·  " + fechaReserva.getValue() + " a las " + comboHora.getValue() + "\nPreautorización de " + importe + " € creada.");
        onLimpiar(); actualizarMapa();
    }

    @FXML
    public void onLimpiar() {
        txtNombre.clear(); txtApellidos.clear(); txtTelefono.clear(); txtEmail.clear();
        txtObservaciones.clear(); txtMesaSeleccionada.clear();
        if (txtPeticiones  != null) txtPeticiones.clear();
        if (comboOcasion   != null) comboOcasion.setValue("Ninguna");
        fechaReserva.setValue(null); comboHora.setValue(null);
        idsIntolSelec.clear(); listaAlergias.refresh();
        spinnerComensales.getValueFactory().setValue(1);
        flowMapa.setVisible(false); idMesaSeleccionada = -1;
        lblInfoMesa.setText(""); lblAlergias.setText("Ninguna seleccionada");
        lblEstadoMapa.setText("Selecciona fecha y hora para ver disponibilidad");
    }

    private boolean validar() {
        if (txtNombre.getText().isBlank())    { alerta(Alert.AlertType.WARNING, "El nombre es obligatorio.");       txtNombre.requestFocus();    return false; }
        if (txtApellidos.getText().isBlank()) { alerta(Alert.AlertType.WARNING, "Los apellidos son obligatorios."); txtApellidos.requestFocus(); return false; }
        if (txtTelefono.getText().isBlank())  { alerta(Alert.AlertType.WARNING, "El teléfono es obligatorio.");     txtTelefono.requestFocus();  return false; }
        if (fechaReserva.getValue() == null)  { alerta(Alert.AlertType.WARNING, "Selecciona una fecha.");            return false; }
        if (comboHora.getValue() == null || comboHora.getValue().startsWith("──")) { alerta(Alert.AlertType.WARNING, "Selecciona turno y hora."); return false; }
        if (idMesaSeleccionada == -1) { alerta(Alert.AlertType.WARNING, "Selecciona una mesa del mapa.\n(Primero elige fecha y hora)"); return false; }
        return true;
    }

    private void alerta(Alert.AlertType t, String msg) {
        Alert a = new Alert(t); a.setTitle("NEXERVO"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
