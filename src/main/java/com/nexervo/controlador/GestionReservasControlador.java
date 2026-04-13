package com.nexervo.controlador;

import com.nexervo.datos.ClienteDAO;
import com.nexervo.datos.MesaDAO;
import com.nexervo.datos.ReservaDAO;
import com.nexervo.modelo.Intolerancia;
import com.nexervo.modelo.Mesa;
import com.nexervo.modelo.Reserva;
import com.nexervo.modelo.Usuario;
import com.nexervo.servicio.PdfService;
import com.nexervo.servicio.ReservaServicio;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controlador de la vista de gestión de reservas.
 *
 * Esta vista es la que más horas me ha llevado del proyecto.
 * Es básicamente la pantalla que usaría el maître durante el servicio:
 * buscar reservas por fecha, ver los detalles, modificarlas, cancelarlas...
 *
 * Las funciones que añadí en v3 son las que más sentido tienen en la práctica:
 *  - Ocasión especial: en sala siempre anotas si es cumpleaños o aniversario
 *    para preparar algo especial (decoración, tarta, etc.)
 *  - No se presentó: diferente de cancelar — el cliente no apareció sin avisar.
 *    La distinción importa porque afecta a la penalización y a las estadísticas.
 *  - Hoja del día: el parte de sala que el maître da a cocina antes del servicio.
 *    Lo diseñé basándome en cómo lo hacíamos nosotros a mano en papel.
 */
public class GestionReservasControlador implements PrincipalControlador.NecesitaUsuario {

    @FXML private DatePicker                   dateBusqueda;
    @FXML private TableView<Reserva>           tabla;
    @FXML private TableColumn<Reserva,String>  colHora;
    @FXML private TableColumn<Reserva,String>  colMesa;
    @FXML private TableColumn<Reserva,String>  colCliente;
    @FXML private TableColumn<Reserva,String>  colComensales;
    @FXML private TableColumn<Reserva,String>  colOcasion;
    @FXML private TableColumn<Reserva,String>  colEstado;

    @FXML private VBox   panelDetalle;
    @FXML private VBox   panelPlaceholder;
    @FXML private VBox   panelAlerta;
    @FXML private VBox   panelOcasionAlerta;
    @FXML private Label  lblCliente;
    @FXML private Label  lblAlerta;
    @FXML private Label  lblOcasionAlerta;

    @FXML private DatePicker       editFecha;
    @FXML private ComboBox<String> editHora;
    @FXML private ComboBox<String> editMesa;
    @FXML private Spinner<Integer> editComensales;
    @FXML private ComboBox<String> editOcasion;
    @FXML private TextArea         editPeticiones;
    @FXML private TextArea         editObservaciones;
    @FXML private ListView<Intolerancia> editIntolerancias;
    @FXML private Label            lblEstadoEdit;
    @FXML private Button           btnGuardar;
    @FXML private Button           btnCancelar;
    @FXML private Button           btnNoPresentado;
    @FXML private Button           btnFinalizar;

    private final ReservaDAO      reservaDAO      = new ReservaDAO();
    private final ClienteDAO      clienteDAO      = new ClienteDAO();
    private final MesaDAO         mesaDAO         = new MesaDAO();
    private final ReservaServicio reservaServicio = new ReservaServicio();

    private Reserva seleccionada;
    private Usuario usuarioActivo;
    private List<Intolerancia> catalogoIntol = new ArrayList<>();
    private final Set<Integer> idsIntolSelec  = new HashSet<>();

    private static final List<String> HORAS_COMIDA = List.of("13:00","13:30","14:00","14:30","15:00");
    private static final List<String> HORAS_CENA   = List.of("20:30","21:00","21:30","22:00","22:30");
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void setUsuario(Usuario u) {
        this.usuarioActivo = u;
        if (btnFinalizar != null) {
            btnFinalizar.setVisible(u != null && u.esAdmin());
            btnFinalizar.setManaged(u != null && u.esAdmin());
        }
        if (btnNoPresentado != null) {
            btnNoPresentado.setVisible(u != null && u.esAdmin());
            btnNoPresentado.setManaged(u != null && u.esAdmin());
        }
    }

    @FXML
    public void initialize() {
        configurarTabla();
        configurarFormEdicion();

        panelDetalle.setVisible(false);
        panelPlaceholder.setVisible(true);
        panelAlerta.setVisible(false);
        panelOcasionAlerta.setVisible(false);
        panelOcasionAlerta.setManaged(false);

        dateBusqueda.setValue(LocalDate.now());
        buscar();
    }

    // ════════════════════════════════════════════════════════════
    // CONFIGURACIÓN
    // ════════════════════════════════════════════════════════════

    private void configurarTabla() {
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colHora.setCellValueFactory(c       -> new SimpleStringProperty(c.getValue().getHoraReserva().toString()));
        colMesa.setCellValueFactory(c       -> new SimpleStringProperty(c.getValue().getNumeroMesa()));
        colCliente.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getNombreCliente()));
        colComensales.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getComensales())));
        colOcasion.setCellValueFactory(c    -> new SimpleStringProperty(c.getValue().getOcasion()));
        colEstado.setCellValueFactory(c     -> new SimpleStringProperty(c.getValue().getEstadoReserva()));

        tabla.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Reserva r, boolean empty) {
                super.updateItem(r, empty);
                getStyleClass().removeAll("fila-cancelada","fila-finalizada","fila-no-presentado","fila-ocasion");
                if (!empty && r != null) {
                    switch (r.getEstadoReserva()) {
                        case "CANCELADA"  -> getStyleClass().add("fila-cancelada");
                        case "FINALIZADA" -> getStyleClass().add("fila-finalizada");
                        case "No presentado"    -> getStyleClass().add("fila-no-presentado");
                        default -> {
                            if (r.tieneOcasionEspecial()) getStyleClass().add("fila-ocasion");
                        }
                    }
                }
            }
        });

        tabla.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, nueva) -> {
                if (nueva != null) cargarDetalle(nueva);
                else {
                    panelDetalle.setVisible(false);
                    panelPlaceholder.setVisible(true);
                    panelAlerta.setVisible(false);
                }
            }
        );
    }

    private void configurarFormEdicion() {
        // Spinner comensales
        var f = new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 1);
        f.setConverter(new StringConverter<>() {
            @Override public String toString(Integer v)   { return v == null ? "1" : v.toString(); }
            @Override public Integer fromString(String s) {
                try { return Math.max(1, Integer.parseInt(s.trim())); } catch (NumberFormatException e) { return 1; }
            }
        });
        editComensales.setValueFactory(f);
        editComensales.setEditable(true);

        // Fechas pasadas deshabilitadas
        editFecha.setDayCellFactory(p -> new DateCell() {
            @Override public void updateItem(LocalDate d, boolean empty) {
                super.updateItem(d, empty);
                setDisable(d.isBefore(LocalDate.now()));
            }
        });

        editFecha.valueProperty().addListener((o,v,n) -> {
            if (n != null) {
                ObservableList<String> items = FXCollections.observableArrayList();
                items.addAll(HORAS_COMIDA); items.addAll(HORAS_CENA);
                editHora.setItems(items);
            }
        });

        // Mesas
        editMesa.setItems(FXCollections.observableArrayList(
            mesaDAO.listarMesas().stream().map(Mesa::getNumeroMesa).collect(Collectors.toList())
        ));

        // Ocasión especial
        editOcasion.setItems(FXCollections.observableArrayList(Reserva.OCASIONES));

        // Intolerancias con checkbox.
        // Uso un Set<Integer> para los IDs seleccionados porque JavaFX recicla las celdas
        // del ListView y si usara el estado interno del CheckBox se perdía la selección
        // al hacer scroll. Así los IDs persisten independientemente de qué celdas están visibles.
        catalogoIntol = clienteDAO.obtenerCatalogoIntolerancias();

        ObservableList<Intolerancia> items = FXCollections.observableArrayList();
        items.add(new Intolerancia(-1, "── 14 ALÉRGENOS OFICIALES UE ──", "CABECERA"));
        catalogoIntol.stream().filter(i -> "ALERGENO_UE".equals(i.getTipo())).forEach(items::add);
        items.add(new Intolerancia(-2, "── INTOLERANCIAS COMUNES ──", "CABECERA"));
        catalogoIntol.stream().filter(i -> "INTOLERANCIA".equals(i.getTipo())).forEach(items::add);
        editIntolerancias.setItems(items);

        editIntolerancias.setCellFactory(lv -> new ListCell<Intolerancia>() {
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
                    });
                    setGraphic(cb);
                }
            }
        });
    }

    // ════════════════════════════════════════════════════════════
    // BÚSQUEDA
    // ════════════════════════════════════════════════════════════

    @FXML public void onBuscar() { buscar(); }

    private void buscar() {
        if (dateBusqueda.getValue() == null) return;
        tabla.getSelectionModel().clearSelection();
        seleccionada = null;
        panelDetalle.setVisible(false);
        panelPlaceholder.setVisible(true);
        panelAlerta.setVisible(false);
        panelOcasionAlerta.setVisible(false);
        panelOcasionAlerta.setManaged(false);
        tabla.setItems(FXCollections.observableArrayList(
            reservaServicio.obtenerReservasDelDia(dateBusqueda.getValue())
        ));
    }

    // ════════════════════════════════════════════════════════════
    // CARGA DE DETALLE
    // ════════════════════════════════════════════════════════════

    private void cargarDetalle(Reserva r) {
        seleccionada = r;

        lblCliente.setText(r.getNombreCliente() + "  ·  Mesa " + r.getNumeroMesa()
            + "  ·  " + r.getFechaReserva().format(FMT) + " " + r.getHoraReserva());

        editFecha.setValue(r.getFechaReserva());

        ObservableList<String> horas = FXCollections.observableArrayList();
        horas.addAll(HORAS_COMIDA); horas.addAll(HORAS_CENA);
        editHora.setItems(horas);
        editHora.setValue(r.getHoraReserva().toString());

        editMesa.setValue(r.getNumeroMesa());
        editComensales.getValueFactory().setValue(r.getComensales());
        editOcasion.setValue(r.getOcasion());
        editPeticiones.setText(r.getPeticionesEspeciales() != null ? r.getPeticionesEspeciales() : "");
        editObservaciones.setText(r.getObservaciones() != null ? r.getObservaciones() : "");

        // Intolerancias del cliente
        List<Intolerancia> suyas = clienteDAO.obtenerIntoleranciasPorCliente(r.getIdCliente());
        idsIntolSelec.clear();
        for (Intolerancia intol : suyas) idsIntolSelec.add(intol.getIdIntolerancia());
        editIntolerancias.refresh();

        // Panel alérgenos
        if (suyas.isEmpty()) {
            panelAlerta.setVisible(false);
        } else {
            lblAlerta.setText("CLIENTE CON ALERGIAS:\n- "
                + suyas.stream().map(Intolerancia::getNombre).collect(Collectors.joining("\n- ")));
            panelAlerta.setVisible(true);
        }

        // Panel ocasión especial
        if (r.tieneOcasionEspecial()) {
            String peticion = (r.getPeticionesEspeciales() != null && !r.getPeticionesEspeciales().isBlank())
                ? "\nPetición: " + r.getPeticionesEspeciales()
                : "";
            lblOcasionAlerta.setText("OCASIÓN ESPECIAL: " + r.getOcasion() + peticion);
            panelOcasionAlerta.setVisible(true);
            panelOcasionAlerta.setManaged(true);
        } else {
            panelOcasionAlerta.setVisible(false);
            panelOcasionAlerta.setManaged(false);
        }

        boolean confirmada = "CONFIRMADA".equals(r.getEstadoReserva());
        btnGuardar.setDisable(!confirmada);
        btnCancelar.setDisable(!confirmada);
        if (btnFinalizar != null) btnFinalizar.setDisable(!confirmada);
        if (btnNoPresentado    != null) btnNoPresentado.setDisable(!confirmada);
        lblEstadoEdit.setText(confirmada ? "" : "Solo se pueden editar reservas CONFIRMADAS.");

        panelPlaceholder.setVisible(false);
        panelDetalle.setVisible(true);
    }

    // ════════════════════════════════════════════════════════════
    // ACCIONES
    // ════════════════════════════════════════════════════════════

    @FXML
    public void onGuardar() {
        if (seleccionada == null || btnGuardar.isDisabled()) return;
        if (editFecha.getValue() == null) { info("Selecciona una fecha."); return; }
        if (editHora.getValue() == null)  { info("Selecciona una hora.");  return; }
        if (editMesa.getValue() == null)  { info("Selecciona una mesa.");  return; }

        int idMesaNueva = mesaDAO.listarMesas().stream()
            .filter(m -> m.getNumeroMesa().equals(editMesa.getValue()))
            .findFirst().map(Mesa::getIdMesa).orElse(seleccionada.getIdMesa());

        LocalTime horaEditada = LocalTime.parse(editHora.getValue());

        if (reservaServicio.hayConflicto(idMesaNueva, editFecha.getValue(), horaEditada, seleccionada.getIdReserva())) {
            info("Esa mesa ya está reservada para esa fecha y hora.\nElige otra mesa o cambia el turno.");
            return;
        }

        seleccionada.setFechaReserva(editFecha.getValue());
        seleccionada.setHoraReserva(horaEditada);
        seleccionada.setIdMesa(idMesaNueva);
        seleccionada.setComensales(editComensales.getValue());
        seleccionada.setOcasion(editOcasion.getValue() != null ? editOcasion.getValue() : "Ninguna");
        seleccionada.setPeticionesEspeciales(editPeticiones.getText().trim());
        seleccionada.setObservaciones(editObservaciones.getText().trim());

        boolean ok = reservaDAO.actualizarReserva(seleccionada);

        if (ok) {
            int idCli = seleccionada.getIdCliente();
            for (Intolerancia intol : catalogoIntol) {
                if (idsIntolSelec.contains(intol.getIdIntolerancia()))
                    clienteDAO.vincularIntolerancia(idCli, intol.getIdIntolerancia());
                else
                    clienteDAO.desvincularIntolerancia(idCli, intol.getIdIntolerancia());
            }
            info("Reserva actualizada correctamente.");
            dateBusqueda.setValue(seleccionada.getFechaReserva());
            final int idGuardado = seleccionada.getIdReserva();
            buscar();
            tabla.getItems().stream()
                .filter(res -> res.getIdReserva() == idGuardado)
                .findFirst()
                .ifPresent(res -> tabla.getSelectionModel().select(res));
        } else {
            info("No se pudo actualizar la reserva.");
        }
    }

    @FXML
    public void onCancelar() {
        if (seleccionada == null || btnCancelar.isDisabled()) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("NEXERVO"); confirm.setHeaderText("Cancelar reserva");
        confirm.setContentText("¿Confirmas la cancelación de la reserva de "
            + seleccionada.getNombreCliente() + "?\n\nEsta acción no se puede deshacer.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                reservaDAO.cambiarEstado(seleccionada.getIdReserva(), "CANCELADA");
                buscar();
            }
        });
    }

    /**
     * Registra el no se presentó: el cliente no se presentó sin avisar.
     *
     * Es importante distinguirlo de Cancelar:
     * - Cancelar = el cliente avisa con antelación, la mesa se puede reasignar
     * - No se presentó = el cliente no aparece, la mesa se queda vacía durante el servicio
     *
     * La diferencia en ingresos puede ser significativa, sobre todo en turnos con lista de espera.
     * La preautorización queda retenida como compensación parcial.
     */
    @FXML
    public void onNoPresentado() {
        if (seleccionada == null || (btnNoPresentado != null && btnNoPresentado.isDisabled())) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("NEXERVO"); confirm.setHeaderText("Registrar No se presentó");
        confirm.setContentText("¿Confirmar que el cliente " + seleccionada.getNombreCliente()
            + " no se ha presentado?\n\n"
            + "La preautorización de pago quedará retenida como penalización.\n"
            + "Quedará registrado en el historial del cliente.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                reservaDAO.marcarNoPresentado(seleccionada.getIdReserva());
                buscar();
            }
        });
    }

    @FXML
    public void onFinalizar() {
        if (seleccionada == null) return;
        reservaDAO.cambiarEstado(seleccionada.getIdReserva(), "FINALIZADA");
        buscar();
    }

    /**
     * Genera la "Hoja del día" — el parte de sala que el maître entrega a cocina.
     *
     * Este fue el método que más me gustó implementar porque lo vi útil de verdad.
     * En los restaurantes donde he trabajado esto se hacía a mano en una libreta o
     * en un Word, y con esta función queda generado automáticamente en segundos.
     *
     * El formato incluye turno de comida y cena por separado, con los alérgenos
     * y peticiones especiales de cada mesa. Se puede copiar y enviar por WhatsApp
     * al cocinero o imprimirlo.
     */
    @FXML
    public void onHojaDelDia() {
        LocalDate fecha = dateBusqueda.getValue();
        if (fecha == null) fecha = LocalDate.now();

        List<Reserva> reservas = reservaDAO.obtenerPorFecha(fecha);
        if (reservas.isEmpty()) {
            info("No hay reservas para " + fecha.format(FMT));
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("══════════════════════════════════════\n");
        sb.append("  NEXERVO · PARTE DE SALA\n");
        sb.append("  ").append(fecha.format(DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy",
            new java.util.Locale("es")))).append("\n");
        sb.append("══════════════════════════════════════\n\n");

        // Separar comida / cena
        List<Reserva> comidas = reservas.stream()
            .filter(r -> r.getHoraReserva().getHour() < 17 && "CONFIRMADA".equals(r.getEstadoReserva()))
            .collect(Collectors.toList());
        List<Reserva> cenas = reservas.stream()
            .filter(r -> r.getHoraReserva().getHour() >= 17 && "CONFIRMADA".equals(r.getEstadoReserva()))
            .collect(Collectors.toList());

        if (!comidas.isEmpty()) {
            sb.append("┌─ COMIDA ─────────────────────────────\n");
            comidas.forEach(r -> sb.append(formatarFila(r)));
            sb.append("\n");
        }
        if (!cenas.isEmpty()) {
            sb.append("┌─ CENA ───────────────────────────────\n");
            cenas.forEach(r -> sb.append(formatarFila(r)));
            sb.append("\n");
        }

        long totalPax = reservas.stream()
            .filter(r -> "CONFIRMADA".equals(r.getEstadoReserva()))
            .mapToLong(Reserva::getComensales).sum();
        long totalMesas = reservas.stream()
            .filter(r -> "CONFIRMADA".equals(r.getEstadoReserva())).count();

        sb.append("──────────────────────────────────────\n");
        sb.append(String.format("  TOTAL: %d mesas · %d cubiertos\n", totalMesas, totalPax));
        sb.append("══════════════════════════════════════\n");

        // Capturar listas para el PDF antes de mostrar el diálogo
        final List<Reserva> comidasPdf = comidas;
        final List<Reserva> cenasPdf   = cenas;
        final long totalPaxPdf         = totalPax;
        final LocalDate fechaPdf       = fecha;

        // Mostrar en diálogo con TextArea copiable
        TextArea ta = new TextArea(sb.toString());
        ta.setEditable(false);
        ta.setWrapText(false);
        ta.setPrefSize(520, 380);
        ta.setStyle("-fx-font-family:'Courier New',monospace; -fx-font-size:12px;"
            + "-fx-background-color:#0F0530; -fx-text-fill:#E8E8F0;"
            + "-fx-border-color:#3D2E8A;");

        // Botón para exportar a PDF
        ButtonType btnExportarPdf = new ButtonType("Exportar PDF", ButtonBar.ButtonData.LEFT);

        Alert dlg = new Alert(Alert.AlertType.INFORMATION);
        dlg.setTitle("NEXERVO · Hoja del día");
        dlg.setHeaderText("Parte de sala — " + fecha.format(FMT));
        dlg.getDialogPane().setContent(ta);
        dlg.getDialogPane().setPrefWidth(560);
        dlg.getButtonTypes().add(btnExportarPdf);
        dlg.getDialogPane().getStylesheets().add(
            getClass().getResource("/Estilo.css").toExternalForm()
        );

        dlg.showAndWait().ifPresent(respuesta -> {
            if (respuesta == btnExportarPdf) {
                try {
                    String ruta = PdfService.generarHojaDelDia(
                        fechaPdf, comidasPdf, cenasPdf, totalPaxPdf);
                    info("PDF guardado en:\n" + ruta);
                } catch (Exception e) {
                    System.err.println("Error al generar PDF: " + e.getMessage());
                    alerta("No se pudo generar el PDF.\nDetalle: " + e.getMessage());
                }
            }
        });
    }

    // Formatea una reserva como una fila del parte de sala
    // Cada llamada hace una consulta a BD para los alérgenos — podría optimizarse
    // cargando todos los alérgenos antes del bucle, pero para el volumen de un
    // restaurante pequeño no merece la pena complicarlo ahora
    private String formatarFila(Reserva r) {
        // Obtener alérgenos del cliente
        List<Intolerancia> intols = clienteDAO.obtenerIntoleranciasPorCliente(r.getIdCliente());
        String alergenos = intols.isEmpty() ? "—"
            : intols.stream().map(i -> i.getNombre().split(" ")[0]).collect(Collectors.joining(", "));

        StringBuilder fila = new StringBuilder();
        fila.append(String.format("│ %s  Mesa %-4s  %-20s  %2d pax\n",
            r.getHoraReserva(), r.getNumeroMesa(),
            truncar(r.getNombreCliente(), 20), r.getComensales()));

        if (r.tieneOcasionEspecial()) {
            fila.append("│   Ocasion: ").append(r.getOcasion()).append("\n");
        }
        if (r.getPeticionesEspeciales() != null && !r.getPeticionesEspeciales().isBlank()) {
            fila.append("│   Peticion: ").append(r.getPeticionesEspeciales()).append("\n");
        }
        if (!intols.isEmpty()) {
            fila.append("│   ALERGIAS: ").append(alergenos).append("\n");
        }
        fila.append("│\n");
        return fila.toString();
    }

    private String truncar(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    // ════════════════════════════════════════════════════════════
    // UTILIDADES
    // ════════════════════════════════════════════════════════════

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("NEXERVO"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void alerta(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("NEXERVO"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
