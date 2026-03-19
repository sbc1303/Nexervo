package com.nexervo.controlador;

import datos.ClienteDAO;
import datos.ReservaDAO;
import modelo.Cliente;
import modelo.Reserva;
import modelo.ReservaView;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controlador de la pantalla de listado y gestión de reservas.
 */
public class ListadoControlador {

    @FXML private TableView<ReservaView>              tablaReservas;
    @FXML private TableColumn<ReservaView, Number>    colId;
    @FXML private TableColumn<ReservaView, String>    colNombre, colTelefono, colEmail,
                                                       colMesa, colFecha, colTurno,
                                                       colPersonas, colAlergia, colNotas;
    @FXML private DatePicker filtroFecha;
    @FXML private TextField  filtroBusqueda;
    @FXML private Label      lblTotal;

    private ReservaDAO reservaDAO;
    private ClienteDAO clienteDAO;

    @FXML
    public void initialize() {
        reservaDAO = new ReservaDAO();
        clienteDAO = new ClienteDAO();

        // Enlazar columnas con properties
        colId.setCellValueFactory(c       -> c.getValue().idReservaProperty());
        colNombre.setCellValueFactory(c   -> c.getValue().nombreProperty());
        colTelefono.setCellValueFactory(c -> c.getValue().telefonoProperty());
        colEmail.setCellValueFactory(c    -> c.getValue().emailProperty());
        colMesa.setCellValueFactory(c     -> c.getValue().mesaProperty());
        colFecha.setCellValueFactory(c    -> c.getValue().fechaProperty());
        colTurno.setCellValueFactory(c    -> c.getValue().turnoProperty());
        colPersonas.setCellValueFactory(c -> c.getValue().personasProperty());
        colAlergia.setCellValueFactory(c  -> c.getValue().alergiaProperty());
        colNotas.setCellValueFactory(c    -> c.getValue().notasProperty());

        // Colores de filas según alergias
        tablaReservas.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(ReservaView item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("fila-alergia","fila-normal");
                if (item != null && !item.getAlergia().equalsIgnoreCase("Ninguna")) {
                    getStyleClass().add("fila-alergia");
                } else {
                    getStyleClass().add("fila-normal");
                }
            }
        });

        cargarTodas();
    }

    // ----------------------------------------------------------------
    private void cargarTodas() {
        List<ReservaView> lista = reservaDAO.listarReservasView();
        tablaReservas.setItems(FXCollections.observableArrayList(lista));
        lblTotal.setText("Total: " + lista.size() + " reserva(s)");
    }

    @FXML
    public void onBuscarClick() {
        String texto = filtroBusqueda.getText();
        String fecha = filtroFecha.getValue() != null
            ? filtroFecha.getValue().format(DateTimeFormatter.ISO_LOCAL_DATE) : null;
        List<ReservaView> resultado = reservaDAO.buscarReservas(texto, fecha);
        tablaReservas.setItems(FXCollections.observableArrayList(resultado));
        lblTotal.setText("Resultados: " + resultado.size());
    }

    @FXML
    public void onMostrarTodas() {
        filtroBusqueda.clear();
        filtroFecha.setValue(null);
        cargarTodas();
    }

    // ----------------------------------------------------------------
    @FXML
    public void onEditarReserva() {
        ReservaView sel = tablaReservas.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(Alert.AlertType.WARNING, "Selecciona una reserva de la tabla."); return; }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/editar_reserva.fxml"));
            Stage stage = new Stage();
            stage.setTitle("NEXERVO – Editar Reserva");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(loader.load(), 680, 520));

            EditarReservaControlador ctrl = loader.getController();
            ctrl.cargarReserva(sel);

            stage.showAndWait();
            cargarTodas(); // Refrescar tras editar
        } catch (IOException e) {
            e.printStackTrace();
            alerta(Alert.AlertType.ERROR, "No se pudo abrir el editor.");
        }
    }

    @FXML
    public void onCancelarReserva() {
        ReservaView sel = tablaReservas.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(Alert.AlertType.WARNING, "Selecciona una reserva de la tabla."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Cancelar la reserva de " + sel.getNombre() + " el " + sel.getFecha() + " a las " + sel.getTurno() + "?",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("NEXERVO"); confirm.setHeaderText("Confirmar cancelación");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                if (reservaDAO.cancelarReserva(sel.getIdReserva())) {
                    alerta(Alert.AlertType.INFORMATION, "Reserva de " + sel.getNombre() + " cancelada.");
                    cargarTodas();
                } else {
                    alerta(Alert.AlertType.ERROR, "Error al cancelar la reserva.");
                }
            }
        });
    }

    @FXML
    public void onVerCliente() {
        ReservaView sel = tablaReservas.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(Alert.AlertType.WARNING, "Selecciona una reserva primero."); return; }

        Cliente c = clienteDAO.obtenerClientePorId(sel.getIdCliente());
        if (c == null) { alerta(Alert.AlertType.ERROR, "Cliente no encontrado."); return; }

        List<String> intol = clienteDAO.obtenerIntoleranciasDeCliente(c.getIdCliente());
        String intolStr = intol.isEmpty() ? "Ninguna registrada" : String.join(", ", intol);

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("NEXERVO – Ficha Cliente");
        info.setHeaderText(c.getNombre());
        info.setContentText(
            "📞 Teléfono: " + c.getTelefono() + "\n" +
            "📧 Email: " + (c.getEmail() != null && !c.getEmail().isBlank() ? c.getEmail() : "No indicado") + "\n" +
            "⚠️ Intolerancias: " + intolStr + "\n\n" +
            "📋 Notas: " + (c.getObservaciones() != null ? c.getObservaciones() : "")
        );
        info.showAndWait();
    }

    @FXML
    public void onCerrar() {
        ((Stage) tablaReservas.getScene().getWindow()).close();
    }

    private void alerta(Alert.AlertType tipo, String msg) {
        Alert a = new Alert(tipo);
        a.setTitle("NEXERVO"); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}
