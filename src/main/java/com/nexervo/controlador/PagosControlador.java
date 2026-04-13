package com.nexervo.controlador;

import com.nexervo.datos.PagoDAO;
import com.nexervo.datos.ReservaDAO;
import com.nexervo.modelo.PreautorizacionPago;
import com.nexervo.modelo.Reserva;
import com.nexervo.modelo.Usuario;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * PagosControlador V5 — FIX 4: lógica de preautorización CORREGIDA definitivamente.
 *
 * Cómo funciona la preautorización anti no se presentó:
 *   Al confirmar una reserva se retiene (preautoriza) 10€ por comensal.
 *   El dinero NO se cobra en ese momento, solo se reserva en la tarjeta.
 *
 * Cuando llega el día de la reserva, el personal decide:
 *
 *   Cliente SE PRESENTA → "DEVOLVER preautorización"
 *      El cliente vino y consumió → se LIBERA la retención, no se cobra nada.
 *      Estado en BD: DEVUELTA
 *
 *   Cliente NO SE PRESENTA → "COBRAR preautorización"
 *      El cliente no vino → se COBRA el importe retenido como penalización.
 *      Estado en BD: AUTORIZADA (cobro efectivo)
 */
public class PagosControlador implements PrincipalControlador.NecesitaUsuario {

    @FXML private DatePicker                   datePagos;
    @FXML private TableView<Reserva>           tabla;
    @FXML private TableColumn<Reserva,String>  colCliente;
    @FXML private TableColumn<Reserva,String>  colMesa;
    @FXML private TableColumn<Reserva,String>  colHora;
    @FXML private TableColumn<Reserva,String>  colPax;
    @FXML private TableColumn<Reserva,String>  colEstadoPago;

    @FXML private Label  lblPagoCliente;
    @FXML private Label  lblPagoImporte;
    @FXML private Label  lblPagoEstado;
    @FXML private Label  lblPagoFecha;

    // FIX 4: nombres de botones actualizados para reflejar la lógica correcta
    @FXML private Button btnDevolver;   // Cliente SE PRESENTÓ → liberar retención
    @FXML private Button btnCobrar;     // Cliente NO VINO → cobrar penalización

    private final ReservaDAO reservaDAO = new ReservaDAO();
    private final PagoDAO    pagoDAO    = new PagoDAO();

    private Reserva             reservaActual;
    private PreautorizacionPago pagoActual;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override public void setUsuario(Usuario u) { }

    @FXML
    public void initialize() {
        configurarTabla();
        ocultarDetalle();
        datePagos.setValue(LocalDate.now());
        cargar();
    }

    private void configurarTabla() {
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colCliente.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNombreCliente()));
        colMesa.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getNumeroMesa()));
        colHora.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getHoraReserva().toString()));
        colPax.setCellValueFactory(c -> new SimpleStringProperty(String.valueOf(c.getValue().getComensales())));
        colEstadoPago.setCellValueFactory(c -> {
            PreautorizacionPago p = pagoDAO.obtenerPorReserva(c.getValue().getIdReserva());
            return new SimpleStringProperty(p != null ? p.getEstado() : "SIN PAGO");
        });

        tabla.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Reserva r, boolean empty) {
                super.updateItem(r, empty);
                getStyleClass().removeAll("fila-pago-autorizado","fila-pago-devuelto");
                if (!empty && r != null) {
                    PreautorizacionPago p = pagoDAO.obtenerPorReserva(r.getIdReserva());
                    if (p != null) {
                        // AUTORIZADA = cobrado (no se presentó)
                        if ("AUTORIZADA".equals(p.getEstado())) getStyleClass().add("fila-pago-autorizado");
                        // DEVUELTA = liberado (se presentó)
                        if ("DEVUELTA".equals(p.getEstado()))   getStyleClass().add("fila-pago-devuelto");
                    }
                }
            }
        });

        tabla.getSelectionModel().selectedItemProperty().addListener(
            (o,v,n) -> { if (n != null) mostrarPago(n); else ocultarDetalle(); }
        );
    }

    @FXML public void onCargar() { cargar(); }

    private void cargar() {
        if (datePagos.getValue() == null) return;
        List<Reserva> reservas = reservaDAO.obtenerPorFecha(datePagos.getValue()).stream()
            .filter(r -> "CONFIRMADA".equals(r.getEstadoReserva())
                      || "FINALIZADA".equals(r.getEstadoReserva())
                      || "No presentado".equals(r.getEstadoReserva()))
            .toList();

        // Crear preautorización si no existe
        for (Reserva r : reservas) {
            if (pagoDAO.obtenerPorReserva(r.getIdReserva()) == null) {
                BigDecimal imp = new BigDecimal("10.00").multiply(new BigDecimal(r.getComensales()));
                pagoDAO.crearPreautorizacion(r.getIdReserva(), imp);
            }
        }

        tabla.setItems(FXCollections.observableArrayList(reservas));
        tabla.getSelectionModel().clearSelection();
        ocultarDetalle();
    }

    private void mostrarPago(Reserva r) {
        reservaActual = r;
        pagoActual    = pagoDAO.obtenerPorReserva(r.getIdReserva());
        if (pagoActual == null) { ocultarDetalle(); return; }

        lblPagoCliente.setText(r.getNombreCliente());
        lblPagoImporte.setText(pagoActual.getImporte() + " €");
        lblPagoEstado.setText(pagoActual.getEstado());
        lblPagoFecha.setText(pagoActual.getFechaCreacion() != null
            ? pagoActual.getFechaCreacion().format(FMT) : "—");

        String estado = pagoActual.getEstado();
        boolean pendiente = "PENDIENTE".equals(estado);

        // Botones solo activos cuando la preautorización está PENDIENTE
        btnDevolver.setDisable(!pendiente);
        btnCobrar.setDisable(!pendiente);

        // Si ya fue gestionado, mostrar estado descriptivo para que quede claro
        if (!pendiente) {
            String desc = switch (estado) {
                case "DEVUELTA"   -> "Retención liberada — cliente se presentó";
                case "AUTORIZADA" -> "Penalización cobrada — no se presentó";
                case "RECHAZADA"  -> "Pago rechazado en simulación";
                default           -> estado;
            };
            lblPagoEstado.setText(desc);
        }
    }

    /**
     * Cliente SE PRESENTÓ → liberar la retención (no se cobra nada)
     * y marcar la reserva como FINALIZADA.
     */
    @FXML
    public void onDevolver() {
        if (pagoActual == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("NEXERVO");
        confirm.setHeaderText("Cliente se presentó — liberar retención");
        confirm.setContentText("El cliente " + reservaActual.getNombreCliente()
            + " se presentó a su reserva.\n\n"
            + "Se liberará la retención de " + pagoActual.getImporte() + " €.\n"
            + "No se cobrará ningún importe.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                pagoDAO.cambiarEstado(pagoActual.getIdPago(), "DEVUELTA");
                // La reserva queda finalizada: cliente cumplió
                reservaDAO.cambiarEstado(reservaActual.getIdReserva(), "FINALIZADA");
                info("Retención liberada.\nEl cliente no ha sido cobrado.");
                cargar();
            }
        });
    }

    /**
     * Cliente NO SE PRESENTÓ → cobrar la preautorización como penalización
     * y marcar la reserva como "No presentado".
     */
    @FXML
    public void onCobrar() {
        if (pagoActual == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("NEXERVO");
        confirm.setHeaderText("No se presentó — cobrar penalización");
        confirm.setContentText("El cliente " + reservaActual.getNombreCliente()
            + " NO se presentó a su reserva.\n\n"
            + "Se cobrará la preautorización de " + pagoActual.getImporte() + " €\n"
            + "como penalización por no presentarse.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                pagoDAO.cambiarEstado(pagoActual.getIdPago(), "AUTORIZADA");
                // La reserva refleja que el cliente no se presentó
                reservaDAO.cambiarEstado(reservaActual.getIdReserva(), "No presentado");
                info("Penalización cobrada: " + pagoActual.getImporte() + " €\n"
                    + "El importe retenido ha sido capturado.");
                cargar();
            }
        });
    }

    private void ocultarDetalle() {
        reservaActual = null; pagoActual = null;
        lblPagoCliente.setText("—"); lblPagoImporte.setText("—");
        lblPagoEstado.setText("—");  lblPagoFecha.setText("—");
        if (btnDevolver != null) btnDevolver.setDisable(true);
        if (btnCobrar   != null) btnCobrar.setDisable(true);
    }

    private void info(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle("NEXERVO"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }
}
