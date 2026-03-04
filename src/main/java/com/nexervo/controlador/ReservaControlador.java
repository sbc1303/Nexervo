package com.nexervo.controlador;

import datos.ClienteDAO;
import modelo.Cliente;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;

/**
 * Controlador para la gestión de reservas.
 * Maneja la interacción entre la vista FXML y la lógica de negocio (DAO).
 */
public class ReservaControlador {

    // Elementos de la interfaz mediante FXML
    @FXML private TextField txtNombre;
    @FXML private TextField txtTelefono;
    @FXML private Spinner<Integer> spinnerComensales;
    @FXML private DatePicker fechaReserva;
    @FXML private ComboBox<String> comboHora;
    @FXML private ComboBox<String> comboAlergias;

    // Instancia del DAO para gestionar la persistencia
    private ClienteDAO clienteDAO;

    @FXML
    public void initialize() {
        // Inicializamos la conexión a datos
        this.clienteDAO = new ClienteDAO();

        // Configuración de valores con un mínimo de 1, un máximo de 20, y un valor inicial de 2
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 20, 2);
        spinnerComensales.setValueFactory(valueFactory);

        // Horas disponibles (Simulado, se implementará mas adelante en BBDD)
        ObservableList<String> horas = FXCollections.observableArrayList(
                "13:00", "13:30", "14:00", "14:30", "15:00",
                "20:00", "20:30", "21:00", "21:30", "22:00"
        );
        comboHora.setItems(horas);

        // Lista de alergias o intolerancias (igual que en las horas, se implementará más adelante en BBDD)
        ObservableList<String> alergias = FXCollections.observableArrayList(
                "Ninguna", "Gluten", "Lactosa", "Frutos Secos", "Marisco"
        );
        comboAlergias.setItems(alergias);

        // Valida el teléfono en tiempo real
        // Asegura que solo se introduzcan números y limita la longitud a 9 caracteres (longitud de telefono en español)
        txtTelefono.textProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue.matches("\\d*")) {
                txtTelefono.setText(newValue.replaceAll("[^\\d]", ""));
            }
            if (txtTelefono.getText().length() > 9) {
                txtTelefono.setText(txtTelefono.getText().substring(0, 9));
            }
        });
    }

    /**
     * Gestiona el evento de clic en el botón Confirmar.
     * Recoge datos, valida y guarda en la base de datos.
     */
    @FXML
    protected void onConfirmarClick() {
        // Recogida de datos de los controles
        String nombre = txtNombre.getText();
        String telefono = txtTelefono.getText();
        var fecha = fechaReserva.getValue();
        String hora = comboHora.getValue();
        String alergia = comboAlergias.getValue();

        if (nombre == null || nombre.trim().isEmpty()) {
            mostrarAlerta(AlertType.ERROR, "Error de validación", "El campo nombre es obligatorio.");
            return;
        }

        // Validación estricta de 9 dígitos para el teléfono
        if (telefono == null || telefono.length() != 9) {
            mostrarAlerta(AlertType.ERROR, "Formato incorrecto", "El teléfono debe tener 9 dígitos.");
            return;
        }

        if (fecha == null) {
            mostrarAlerta(AlertType.ERROR, "Faltan datos", "Debe seleccionar una fecha para la reserva.");
            return;
        }

        if (hora == null) {
            mostrarAlerta(AlertType.ERROR, "Faltan datos", "Debe seleccionar una hora.");
            return;
        }

        // --- Creación del objeto y persistencia ---

        try {
            Cliente cliente = new Cliente();
            cliente.setNombre(nombre);
            cliente.setTelefono(telefono);
            // TODO: Implementar campo email en la vista. Usamos placeholder por ahora.
            cliente.setEmail("no-email@registrado.com");

            // Concatenamos los detalles de la reserva en observaciones, ya que la tabla cliente no tiene campos específicos para fecha/hora
            String detallesReserva = String.format("Reserva: %s a las %s. Alergias: %s",
                    fecha.toString(), hora, alergia);
            cliente.setObservaciones(detallesReserva);

            // Llamada al DAO para insertar en MySQL
            boolean registrado = clienteDAO.registrarCliente(cliente);

            if (registrado) {
                mostrarAlerta(AlertType.INFORMATION, "Reserva completada",
                        "El cliente y la reserva se han guardado correctamente.");
                limpiarFormulario();
            } else {
                mostrarAlerta(AlertType.ERROR, "Error en base de datos",
                        "No se pudo guardar el registro. Verifique la conexión.");
            }

        } catch (Exception e) {
            e.printStackTrace(); // Imprimimos traza para depuración
            mostrarAlerta(AlertType.ERROR, "Error inesperado", "Ocurrió un error interno en la aplicación.");
        }
    }

    /**
     * Limpia los campos del formulario para permitir una nueva entrada.
     */
    @FXML
    protected void onLimpiarClick() {
        limpiarFormulario();
    }

    private void limpiarFormulario() {
        txtNombre.clear();
        txtTelefono.clear();
        spinnerComensales.getValueFactory().setValue(2); // Reset a valor por defecto
        fechaReserva.setValue(null);
        comboHora.setValue(null);
        comboAlergias.getSelectionModel().clearSelection();
    }

    // Método auxiliar para mostrar alertas de usuario
    private void mostrarAlerta(AlertType tipo, String titulo, String contenido) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle("Gestión de Reservas");
        alerta.setHeaderText(titulo);
        alerta.setContentText(contenido);
        alerta.showAndWait();
    }
}