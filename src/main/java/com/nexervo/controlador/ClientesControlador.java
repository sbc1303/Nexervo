package com.nexervo.controlador;

import datos.ClienteDAO;
import modelo.Cliente;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

/**
 * Controlador del listado de clientes con historial e intolerancias.
 */
public class ClientesControlador {

    @FXML private TableView<Cliente>            tablaClientes;
    @FXML private TableColumn<Cliente, String>  colNombre, colTelefono, colEmail, colIntolerancias;
    @FXML private TextField  filtroBusqueda;
    @FXML private Label      lblTotal;
    @FXML private TextArea   txtDetalle;

    private ClienteDAO clienteDAO;

    @FXML
    public void initialize() {
        clienteDAO = new ClienteDAO();

        colNombre.setCellValueFactory(c   -> new SimpleStringProperty(c.getValue().getNombre()));
        colTelefono.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTelefono()));
        colEmail.setCellValueFactory(c    -> new SimpleStringProperty(
            c.getValue().getEmail() != null ? c.getValue().getEmail() : ""));
        colIntolerancias.setCellValueFactory(c -> {
            List<String> intol = clienteDAO.obtenerIntoleranciasDeCliente(c.getValue().getIdCliente());
            return new SimpleStringProperty(intol.isEmpty() ? "Ninguna" : String.join(", ", intol));
        });

        // Al seleccionar un cliente, mostrar detalle
        tablaClientes.getSelectionModel().selectedItemProperty().addListener((o, old, sel) -> {
            if (sel != null) mostrarDetalle(sel);
        });

        cargarTodos();
    }

    private void cargarTodos() {
        List<Cliente> lista = clienteDAO.listarClientes();
        tablaClientes.setItems(FXCollections.observableArrayList(lista));
        lblTotal.setText("Total clientes: " + lista.size());
    }

    @FXML
    public void onBuscarClick() {
        String texto = filtroBusqueda.getText().trim();
        if (texto.isBlank()) { cargarTodos(); return; }
        List<Cliente> resultado = clienteDAO.buscarClientes(texto);
        tablaClientes.setItems(FXCollections.observableArrayList(resultado));
        lblTotal.setText("Resultados: " + resultado.size());
    }

    @FXML
    public void onMostrarTodos() {
        filtroBusqueda.clear();
        cargarTodos();
    }

    @FXML
    public void onEliminarCliente() {
        Cliente sel = tablaClientes.getSelectionModel().getSelectedItem();
        if (sel == null) { alerta(Alert.AlertType.WARNING, "Selecciona un cliente."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "¿Eliminar a " + sel.getNombre() + " y todas sus reservas?",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("NEXERVO"); confirm.setHeaderText("Confirmar eliminación");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                if (clienteDAO.eliminarCliente(sel.getIdCliente())) {
                    alerta(Alert.AlertType.INFORMATION, "Cliente eliminado correctamente.");
                    cargarTodos();
                    txtDetalle.clear();
                }
            }
        });
    }

    private void mostrarDetalle(Cliente c) {
        List<String> intol;
        intol = clienteDAO.obtenerIntoleranciasDeCliente(c.getIdCliente());
        String intolStr = intol.isEmpty() ? "Ninguna registrada" : String.join(", ", intol);
        txtDetalle.setText(
            "👤  " + c.getNombre() + "\n" +
            "📞  " + c.getTelefono() + "\n" +
            "📧  " + (c.getEmail() != null && !c.getEmail().isBlank() ? c.getEmail() : "No indicado") + "\n" +
            "⚠️  Intolerancias: " + intolStr + "\n\n" +
            "📋  " + (c.getObservaciones() != null ? c.getObservaciones() : "Sin notas")
        );
    }

    @FXML
    public void onCerrar() {
        ((Stage) tablaClientes.getScene().getWindow()).close();
    }

    private void alerta(Alert.AlertType tipo, String msg) {
        Alert a = new Alert(tipo);
        a.setTitle("NEXERVO"); a.setHeaderText(null); a.setContentText(msg);
        a.showAndWait();
    }
}
