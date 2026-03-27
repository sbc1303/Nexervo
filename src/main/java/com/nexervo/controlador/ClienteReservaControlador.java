package com.nexervo.controlador;

import com.nexervo.Main;
import com.nexervo.datos.ClienteDAO;
import com.nexervo.datos.MesaDAO;
import com.nexervo.datos.PagoDAO;
import com.nexervo.datos.ReservaDAO;
import com.nexervo.modelo.Cliente;
import com.nexervo.modelo.Intolerancia;
import com.nexervo.modelo.Mesa;
import com.nexervo.modelo.Reserva;
import com.nexervo.servicio.EmailService;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ClienteReservaControlador {

    // Paneles
    @FXML private StackPane stackPaneles;
    @FXML private VBox panelFormulario;
    @FXML private VBox panelPago;
    @FXML private VBox panelConfirmacion;
    @FXML private VBox panelMisReservas;

    // Formulario
    @FXML private TextField        txtNombre;
    @FXML private TextField        txtApellidos;
    @FXML private TextField        txtTelefono;
    @FXML private TextField        txtEmail;
    @FXML private Spinner<Integer> spinnerComensales;
    @FXML private DatePicker       fechaReserva;
    @FXML private ComboBox<String> comboHora;
    @FXML private TextField        txtMesaSeleccionada;
    @FXML private Label            lblInfoMesa;
    @FXML private Label            lblEstadoMapa;
    @FXML private FlowPane         flowMapa;
    @FXML private ListView<Intolerancia> listaAlergiasForm;
    @FXML private Label            lblAlergiasForm;
    @FXML private TextArea         txtObservaciones;
    @FXML private VBox             panelClienteHabitual;
    @FXML private Label            lblHistorialCliente;

    // Pago
    @FXML private Label  lblResumenPago;
    @FXML private Label  lblImportePago;
    @FXML private Button btnPagar;
    @FXML private Label  lblProcesando;

    // Confirmacion
    @FXML private Label lblConfirmacion;
    @FXML private Label lblDetalleConfirmacion;

    // Mis reservas
    @FXML private TextField                   txtBuscarTel;
    @FXML private TextField                   txtBuscarEmail;
    @FXML private TableView<Reserva>          tablaMisReservas;
    @FXML private TableColumn<Reserva,String> colMRFecha;
    @FXML private TableColumn<Reserva,String> colMRHora;
    @FXML private TableColumn<Reserva,String> colMRMesa;
    @FXML private TableColumn<Reserva,String> colMRPax;
    @FXML private TableColumn<Reserva,String> colMREstado;
    @FXML private Button btnCancelarMiReserva;
    @FXML private Button btnEditarMiReserva;

    // Editar reserva
    @FXML private VBox      panelEditarReserva;
    @FXML private Label     lblInfoReservaEditar;
    @FXML private Spinner<Integer> spinnerEditComensales;
    @FXML private TextField txtEditMesa;
    @FXML private TextArea  txtEditObservaciones;
    @FXML private VBox      vboxAlergiasEditar;
    @FXML private Label     lblEstadoEdicion;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final MesaDAO    mesaDAO    = new MesaDAO();
    private final PagoDAO    pagoDAO    = new PagoDAO();

    private int        idMesaSeleccionada = -1;
    private String     nombreMesa = "";
    private BigDecimal importePago;
    private int        idReservaCreada = -1;
    private Reserva    miReservaSeleccionada;
    private Cliente    clienteActual;
    private boolean    modoEdicion = false;
    private int        idMesaEdicion = -1;
    private String     numMesaEdicion = "";
    private final List<CheckBox> checkBoxesAlergias = new ArrayList<>();

    // Alergias del formulario de nueva reserva — Set para sobrevivir al reciclaje de celdas
    private List<Intolerancia> catalogoForm = new ArrayList<>();
    private final Set<Integer> idsFormSelec = new HashSet<>();

    private static final List<String> HORAS_COMIDA = List.of("13:00","13:30","14:00","14:30","15:00");
    private static final List<String> HORAS_CENA   = List.of("20:30","21:00","21:30","22:00","22:30");
    private static final BigDecimal   IMP_PAX      = new BigDecimal("10.00");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        configurarSpinner();
        configurarCalendario();
        configurarIntoleranciasForm();
        configurarTelefono();
        configurarTablaMisReservas();
        cargarMapa();
        fechaReserva.valueProperty().addListener((o,v,n) -> { actualizarHoras(n); actualizarMapa(); });
        comboHora.valueProperty().addListener((o,v,n) -> actualizarMapa());
        flowMapa.setVisible(false);
        panelClienteHabitual.setVisible(false);
        panelClienteHabitual.setManaged(false);
        lblEstadoMapa.setText("Selecciona fecha y hora para ver disponibilidad");
        mostrarPanel(panelFormulario);
    }

    // ── Intolerancias del formulario de nueva reserva ────────────
    private void configurarIntoleranciasForm() {
        catalogoForm = clienteDAO.obtenerCatalogoIntolerancias();

        ObservableList<Intolerancia> items = FXCollections.observableArrayList();
        items.add(new Intolerancia(-1, "── 14 ALÉRGENOS OFICIALES UE ──", "CABECERA"));
        catalogoForm.stream().filter(i -> "ALERGENO_UE".equals(i.getTipo())).forEach(items::add);
        items.add(new Intolerancia(-2, "── INTOLERANCIAS COMUNES ──", "CABECERA"));
        catalogoForm.stream().filter(i -> "INTOLERANCIA".equals(i.getTipo())).forEach(items::add);
        listaAlergiasForm.setItems(items);

        listaAlergiasForm.setCellFactory(lv -> new ListCell<Intolerancia>() {
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
                    cb.setSelected(idsFormSelec.contains(item.getIdIntolerancia()));
                    cb.setOnAction(e -> {
                        if (cb.isSelected()) { idsFormSelec.add(item.getIdIntolerancia()); }
                        else                 { idsFormSelec.remove(item.getIdIntolerancia()); }
                        actualizarResumenAlergiasForm();
                    });
                    setGraphic(cb);
                }
            }
        });
    }

    private void actualizarResumenAlergiasForm() {
        int n = idsFormSelec.size();
        if (lblAlergiasForm != null)
            lblAlergiasForm.setText(n == 0 ? "Ninguna seleccionada" : n + " seleccionada(s)");
    }

    /** Precarga las alergias del cliente en el Set y fuerza redibujado del ListView. */
    private void cargarAlergiasFormCliente(int idCliente) {
        List<Intolerancia> suyas = clienteDAO.obtenerIntoleranciasPorCliente(idCliente);
        idsFormSelec.clear();
        for (Intolerancia i : suyas) idsFormSelec.add(i.getIdIntolerancia());
        listaAlergiasForm.refresh(); // fuerza redibujado con el nuevo Set
        actualizarResumenAlergiasForm();
    }

    private void configurarSpinner() {
        var f = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1);
        f.setConverter(new StringConverter<>() {
            @Override public String toString(Integer v) { return v == null ? "1" : v.toString(); }
            @Override public Integer fromString(String s) { try { return Math.max(1, Integer.parseInt(s.trim())); } catch (NumberFormatException e) { return 1; } }
        });
        spinnerComensales.setValueFactory(f); spinnerComensales.setEditable(true);
    }

    private void configurarCalendario() {
        fechaReserva.setDayCellFactory(p -> new DateCell() {
            @Override public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty); setDisable(d.isBefore(LocalDate.now()));
            }
        });
    }

    private void configurarTelefono() {
        txtTelefono.textProperty().addListener((o,v,n) -> { if (!n.matches("\\d*") || n.length() > 9) txtTelefono.setText(v); });
        txtTelefono.focusedProperty().addListener((o,v,focused) -> {
            if (!focused && txtTelefono.getText().length() >= 9) {
                Cliente c = clienteDAO.buscarPorTelefono(txtTelefono.getText());
                if (c != null) {
                    String[] p = c.getNombre().split(" ", 2);
                    txtNombre.setText(p[0]); txtApellidos.setText(p.length > 1 ? p[1] : "");
                    if (c.getEmail() != null) txtEmail.setText(c.getEmail());
                    // Precargar alergias del perfil del cliente
                    cargarAlergiasFormCliente(c.getIdCliente());
                    List<Intolerancia> suyas = clienteDAO.obtenerIntoleranciasPorCliente(c.getIdCliente());
                    lblEstadoMapa.setText(suyas.isEmpty()
                        ? ""
                        : suyas.size() + " alergia(s) cargada(s) de tu perfil");

                    // Mostrar historial de visitas del cliente habitual
                    mostrarHistorialCliente(c);
                } else {
                    // Nuevo cliente — ocultar panel por si venía de uno anterior
                    panelClienteHabitual.setVisible(false);
                    panelClienteHabitual.setManaged(false);
                }
            }
        });
    }

    /**
     * Muestra el panel de bienvenida con el historial del cliente habitual.
     * Consulta sus reservas FINALIZADAS para dar al empleado contexto rápido:
     * número de visitas, última fecha y última mesa usada.
     */
    private void mostrarHistorialCliente(Cliente c) {
        List<Reserva> historial = reservaDAO.obtenerPorCliente(c.getIdCliente());
        List<Reserva> finalizadas = historial.stream()
            .filter(r -> "FINALIZADA".equals(r.getEstadoReserva()))
            .collect(Collectors.toList());

        if (finalizadas.isEmpty()) {
            // Tiene ficha pero aún no ha completado ninguna visita
            panelClienteHabitual.setVisible(false);
            panelClienteHabitual.setManaged(false);
            lblEstadoMapa.setText("Bienvenido de nuevo, " + c.getNombre().split(" ")[0]);
            return;
        }

        // La lista viene ordenada por fecha DESC, la primera finalizada es la más reciente
        Reserva ultima = finalizadas.get(0);
        String fechaUltima = ultima.getFechaReserva()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        String info = String.format(
            "%d visita(s) completada(s)  ·  Última: %s  ·  Mesa %s  ·  %d pax",
            finalizadas.size(), fechaUltima,
            ultima.getNumeroMesa(), ultima.getComensales()
        );

        lblHistorialCliente.setText(info);
        panelClienteHabitual.setVisible(true);
        panelClienteHabitual.setManaged(true);
        lblEstadoMapa.setText("");
    }

    private void configurarTablaMisReservas() {
        colMRFecha.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFechaReserva().format(FMT)));
        colMRHora.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHoraReserva().toString()));
        colMRMesa.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumeroMesa()));
        colMRPax.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getComensales())));
        colMREstado.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getEstadoReserva()));
        tablaMisReservas.getSelectionModel().selectedItemProperty().addListener((o,v,n) -> {
            miReservaSeleccionada = n;
            boolean esConfirmada = n != null && "CONFIRMADA".equals(n.getEstadoReserva());
            btnCancelarMiReserva.setDisable(!esConfirmada);
            btnEditarMiReserva.setDisable(!esConfirmada);
        });
    }

    private void cargarMapa() {
        List<Mesa> mesas = mesaDAO.listarMesas();
        flowMapa.getChildren().clear();
        for (Mesa m : mesas) {
            Button btn = new Button(m.getNumeroMesa());
            btn.setPrefWidth(85); btn.setPrefHeight(55);
            btn.getStyleClass().add("mesa-base");
            Tooltip.install(btn, new Tooltip("Capacidad: " + m.getCapacidad() + " personas"));
            btn.setOnAction(e -> seleccionarMesaEnMapa(btn, m));
            flowMapa.getChildren().add(btn);
        }
    }

    private void seleccionarMesaEnMapa(Button btn, Mesa m) {
        if (btn.isDisabled()) return;   // ocupada → no se puede seleccionar
        flowMapa.getChildren().forEach(n -> { if (n instanceof Button b) b.getStyleClass().remove("mesa-seleccionada"); });
        btn.getStyleClass().add("mesa-seleccionada");
        if (modoEdicion) {
            idMesaEdicion = m.getIdMesa();
            numMesaEdicion = m.getNumeroMesa();
            txtEditMesa.setText(m.getNumeroMesa());
        } else {
            idMesaSeleccionada = m.getIdMesa();
            nombreMesa = m.getNumeroMesa();
            txtMesaSeleccionada.setText(m.getNumeroMesa());
            lblInfoMesa.setText(m.getNumeroMesa() + " · hasta " + m.getCapacidad() + " personas");
        }
    }

    private void actualizarHoras(LocalDate fecha) {
        if (fecha == null) return;
        LocalTime margen = LocalTime.now().plusMinutes(15);
        ObservableList<String> items = FXCollections.observableArrayList();
        List<String> comida = HORAS_COMIDA.stream().filter(h -> !fecha.equals(LocalDate.now()) || LocalTime.parse(h).isAfter(margen)).collect(Collectors.toList());
        List<String> cena   = HORAS_CENA.stream().filter(h -> !fecha.equals(LocalDate.now()) || LocalTime.parse(h).isAfter(margen)).collect(Collectors.toList());
        if (!comida.isEmpty()) { items.add("── TURNO COMIDA ──"); items.addAll(comida); }
        if (!cena.isEmpty())   { items.add("── TURNO CENA ──");   items.addAll(cena); }
        comboHora.setItems(items);
    }

    private void actualizarMapa() {
        if (modoEdicion) return;
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

    @FXML
    public void onConfirmar() {
        if (!validar()) return;
        importePago = IMP_PAX.multiply(new BigDecimal(spinnerComensales.getValue()));
        lblResumenPago.setText("Reserva para " + spinnerComensales.getValue() + " persona(s)\n" + nombreMesa + "  ·  " + fechaReserva.getValue() + " a las " + comboHora.getValue());
        lblImportePago.setText(importePago + " €");
        lblProcesando.setVisible(false); btnPagar.setDisable(false);
        mostrarPanel(panelPago);
    }

    @FXML
    public void onPagar() {
        btnPagar.setDisable(true); lblProcesando.setVisible(true); lblProcesando.setText("Procesando pago...");
        new Thread(() -> {
            try { Thread.sleep(2500); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() -> {
                Cliente cliente = clienteDAO.buscarPorTelefono(txtTelefono.getText());
                if (cliente == null) {
                    cliente = new Cliente(txtNombre.getText().trim() + " " + txtApellidos.getText().trim(), txtTelefono.getText(), txtEmail.getText().isBlank() ? null : txtEmail.getText().trim(), null);
                    int id = clienteDAO.registrarCliente(cliente);
                    if (id == -1) { lblProcesando.setText("Error. Inténtalo de nuevo."); btnPagar.setDisable(false); return; }
                    cliente.setIdCliente(id);
                }
                // Sincronizar alergias desde el Set central
                final int idCli = cliente.getIdCliente();
                for (Intolerancia intol : catalogoForm) {
                    if (idsFormSelec.contains(intol.getIdIntolerancia()))
                        clienteDAO.vincularIntolerancia(idCli, intol.getIdIntolerancia());
                    else
                        clienteDAO.desvincularIntolerancia(idCli, intol.getIdIntolerancia());
                }

                // Validar solapamiento antes de insertar
                LocalTime horaReserva = LocalTime.parse(comboHora.getValue());
                if (reservaDAO.hayConflicto(idMesaSeleccionada, fechaReserva.getValue(), horaReserva, -1)) {
                    lblProcesando.setVisible(false);
                    btnPagar.setDisable(false);
                    alerta("Lo sentimos, esa mesa acaba de ser reservada por otra persona.\nVuelve al formulario y elige otra mesa.");
                    mostrarPanel(panelFormulario);
                    return;
                }

                Reserva r = new Reserva(cliente.getIdCliente(), idMesaSeleccionada, fechaReserva.getValue(), horaReserva, spinnerComensales.getValue(), "Ninguna", null, txtObservaciones.getText().trim());
                idReservaCreada = reservaDAO.crearReserva(r);
                if (idReservaCreada == -1) { lblProcesando.setText("Error al guardar. Inténtalo de nuevo."); btnPagar.setDisable(false); return; }
                int idPago = pagoDAO.crearPreautorizacion(idReservaCreada, importePago);
                if (idPago != -1) pagoDAO.cambiarEstado(idPago, "AUTORIZADA");
                lblConfirmacion.setText("¡Reserva confirmada!");
                lblDetalleConfirmacion.setText("Número de reserva: #" + idReservaCreada + "\n\nNombre: " + cliente.getNombre() + "\nMesa: " + nombreMesa + "\nFecha: " + fechaReserva.getValue() + " a las " + comboHora.getValue() + "\nPersonas: " + spinnerComensales.getValue() + "\n\nImporte pagado: " + importePago + " €\n\n¡Hasta pronto!");
                mostrarPanel(panelConfirmacion);

                // Enviar email de confirmación en hilo secundario para no bloquear la UI
                // Capturamos los valores aquí porque el hilo no puede acceder a controles JavaFX
                final String emailDestino   = cliente.getEmail();
                final String nombreFinal    = cliente.getNombre();
                final int    idFinal        = idReservaCreada;
                final LocalDate fechaFinal  = fechaReserva.getValue();
                final LocalTime horaFinal   = horaReserva;
                final int    paxFinal       = spinnerComensales.getValue();
                final String importeFinal   = importePago.toPlainString();
                final String mesaFinal      = nombreMesa;
                new Thread(() ->
                    EmailService.enviarConfirmacion(emailDestino, nombreFinal, idFinal,
                                                    mesaFinal, fechaFinal, horaFinal,
                                                    paxFinal, importeFinal)
                , "nexervo-email").start();
            });
        }).start();
    }

    // ── Mis Reservas ──────────────────────────────────────────────

    @FXML
    public void onVerMisReservas() { mostrarPanel(panelMisReservas); }

    @FXML
    public void onBuscarMisReservas() {
        String tel   = txtBuscarTel.getText().trim();
        String email = txtBuscarEmail.getText().trim();
        if (tel.isBlank() || email.isBlank()) { alerta("Introduce tu teléfono y email para buscar tus reservas."); return; }
        clienteActual = clienteDAO.buscarPorTelefonoYEmail(tel, email);
        if (clienteActual == null) {
            tablaMisReservas.setItems(FXCollections.observableArrayList());
            alerta("No se encontró ningún cliente con esos datos.\nComprueba tu teléfono y email de registro.");
            return;
        }
        List<Reserva> reservas = reservaDAO.obtenerPorCliente(clienteActual.getIdCliente());
        tablaMisReservas.setItems(FXCollections.observableArrayList(reservas));
        btnCancelarMiReserva.setDisable(true);
        btnEditarMiReserva.setDisable(true);
    }

    @FXML
    public void onEditarMiReserva() {
        if (miReservaSeleccionada == null) return;
        modoEdicion = true;

        // Spinner con valor actual
        var f2 = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, miReservaSeleccionada.getComensales());
        f2.setConverter(new StringConverter<>() {
            @Override public String toString(Integer v) { return v == null ? "1" : v.toString(); }
            @Override public Integer fromString(String s) { try { return Math.max(1, Integer.parseInt(s.trim())); } catch (NumberFormatException e) { return 1; } }
        });
        spinnerEditComensales.setValueFactory(f2);
        spinnerEditComensales.setEditable(true);

        // Mesa actual
        idMesaEdicion  = miReservaSeleccionada.getIdMesa();
        numMesaEdicion = miReservaSeleccionada.getNumeroMesa();
        txtEditMesa.setText(miReservaSeleccionada.getNumeroMesa());

        // Observaciones actuales
        txtEditObservaciones.setText(miReservaSeleccionada.getObservaciones() != null ? miReservaSeleccionada.getObservaciones() : "");

        // Info de la reserva
        lblInfoReservaEditar.setText("Reserva #" + miReservaSeleccionada.getIdReserva()
            + " · " + miReservaSeleccionada.getFechaReserva().format(FMT)
            + " a las " + miReservaSeleccionada.getHoraReserva()
            + " · " + miReservaSeleccionada.getComensales() + " persona(s)");

        // Mapa con disponibilidad para esa fecha y hora
        actualizarMapaParaEdicion();

        // Alergias del cliente
        cargarAlergiasEdicion();

        lblEstadoEdicion.setText("");
        mostrarPanel(panelEditarReserva);
    }

    private void actualizarMapaParaEdicion() {
        flowMapa.setVisible(true);
        String hora = miReservaSeleccionada.getHoraReserva()
                          .format(DateTimeFormatter.ofPattern("HH:mm"));
        List<String> ocupadas = reservaDAO.obtenerMesasOcupadas(
                miReservaSeleccionada.getFechaReserva(), hora);
        String mesaActual = miReservaSeleccionada.getNumeroMesa();
        flowMapa.getChildren().forEach(n -> {
            if (n instanceof Button btn) {
                btn.getStyleClass().removeAll("mesa-libre", "mesa-ocupada", "mesa-seleccionada");
                btn.setDisable(false); btn.setOpacity(1.0);
                if (btn.getText().equals(mesaActual)) {
                    btn.getStyleClass().add("mesa-seleccionada");
                } else if (ocupadas.contains(btn.getText())) {
                    btn.getStyleClass().add("mesa-ocupada");
                    btn.setDisable(true); btn.setOpacity(0.45);
                } else {
                    btn.getStyleClass().add("mesa-libre");
                }
            }
        });
    }

    private void cargarAlergiasEdicion() {
        vboxAlergiasEditar.getChildren().clear();
        checkBoxesAlergias.clear();
        if (clienteActual == null) return;

        List<Intolerancia> catalogo = clienteDAO.obtenerCatalogoIntolerancias();
        List<Intolerancia> actuales = clienteDAO.obtenerIntoleranciasPorCliente(clienteActual.getIdCliente());
        Set<Integer> idsActuales = new HashSet<>();
        for (Intolerancia i : actuales) idsActuales.add(i.getIdIntolerancia());

        String tipoActual = "";
        for (Intolerancia intol : catalogo) {
            if (!intol.getTipo().equals(tipoActual)) {
                tipoActual = intol.getTipo();
                javafx.scene.control.Label sep = new javafx.scene.control.Label(
                    "── " + ("ALERGENO_UE".equals(tipoActual) ? "14 ALÉRGENOS OFICIALES UE" : "INTOLERANCIAS COMUNES") + " ──");
                sep.setStyle("-fx-text-fill:#7070AA; -fx-font-size:10px;");
                vboxAlergiasEditar.getChildren().add(sep);
            }
            CheckBox cb = new CheckBox(intol.getNombre());
            cb.setSelected(idsActuales.contains(intol.getIdIntolerancia()));
            cb.setUserData(intol.getIdIntolerancia());
            cb.setStyle("-fx-text-fill:#E8E8F0;");
            checkBoxesAlergias.add(cb);
            vboxAlergiasEditar.getChildren().add(cb);
        }
    }

    @FXML
    public void onGuardarCambiosReserva() {
        if (miReservaSeleccionada == null || clienteActual == null) return;
        if (idMesaEdicion == -1) { alerta("Selecciona una mesa del mapa."); return; }

        miReservaSeleccionada.setComensales(spinnerEditComensales.getValue());
        miReservaSeleccionada.setIdMesa(idMesaEdicion);
        miReservaSeleccionada.setNumeroMesa(numMesaEdicion);
        miReservaSeleccionada.setObservaciones(txtEditObservaciones.getText().trim());

        boolean ok = reservaDAO.actualizarReserva(miReservaSeleccionada);
        if (!ok) { alerta("No se pudo guardar los cambios. Inténtalo de nuevo."); return; }

        // Actualizar alergias del cliente
        List<Intolerancia> actuales = clienteDAO.obtenerIntoleranciasPorCliente(clienteActual.getIdCliente());
        Set<Integer> idsActuales = new HashSet<>();
        for (Intolerancia i : actuales) idsActuales.add(i.getIdIntolerancia());
        for (CheckBox cb : checkBoxesAlergias) {
            int idIntol = (int) cb.getUserData();
            if (cb.isSelected() && !idsActuales.contains(idIntol))
                clienteDAO.vincularIntolerancia(clienteActual.getIdCliente(), idIntol);
            else if (!cb.isSelected() && idsActuales.contains(idIntol))
                clienteDAO.desvincularIntolerancia(clienteActual.getIdCliente(), idIntol);
        }

        info("¡Reserva modificada con éxito!");
        onVolverAMisReservas();
    }

    @FXML
    public void onVolverAMisReservas() {
        modoEdicion    = false;
        idMesaEdicion  = -1;
        numMesaEdicion = "";
        flowMapa.setVisible(false);
        mostrarPanel(panelMisReservas);
        if (clienteActual != null) onBuscarMisReservas();
    }

    @FXML
    public void onCancelarMiReserva() {
        if (miReservaSeleccionada == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("NEXERVO"); confirm.setHeaderText("Cancelar mi reserva");
        confirm.setContentText("¿Confirmas la cancelación de tu reserva del " + miReservaSeleccionada.getFechaReserva().format(FMT) + "?\n\nSi cancelas con menos de 24h de antelación, la preautorización podría no ser devuelta.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                reservaDAO.cambiarEstado(miReservaSeleccionada.getIdReserva(), "CANCELADA");
                onBuscarMisReservas();
            }
        });
    }

    @FXML
    public void onVolver() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("/vista/login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) stackPaneles.getScene().getWindow();
            stage.setTitle("NEXERVO · Acceso al sistema");
            stage.setScene(scene);
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void mostrarPanel(VBox panel) {
        for (VBox p : new VBox[]{panelFormulario, panelPago, panelConfirmacion, panelMisReservas, panelEditarReserva}) {
            if (p != null) { p.setVisible(false); p.setManaged(false); }
        }
        panel.setVisible(true); panel.setManaged(true);
    }

    private boolean validar() {
        if (txtNombre.getText().isBlank())    { alerta("El nombre es obligatorio.");       txtNombre.requestFocus();    return false; }
        if (txtApellidos.getText().isBlank()) { alerta("Los apellidos son obligatorios."); txtApellidos.requestFocus(); return false; }
        if (txtTelefono.getText().isBlank())  { alerta("El teléfono es obligatorio.");     txtTelefono.requestFocus();  return false; }
        if (fechaReserva.getValue() == null)  { alerta("Selecciona una fecha.");            return false; }
        if (comboHora.getValue() == null || comboHora.getValue().startsWith("──")) { alerta("Selecciona turno y hora."); return false; }
        if (idMesaSeleccionada == -1) { alerta("Selecciona una mesa del mapa.\n(Primero elige fecha y hora)"); return false; }
        return true;
    }

    private void alerta(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("NEXERVO"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("NEXERVO"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
