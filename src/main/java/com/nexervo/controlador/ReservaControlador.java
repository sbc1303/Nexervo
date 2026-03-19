package com.nexervo.controlador;

import datos.ClienteDAO;
import datos.ReservaDAO;
import modelo.Cliente;
import modelo.Intolerancia;
import modelo.Reserva;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador de la vista principal de gestión de reservas.
 * Vinculado a gestiondereservas.fxml mediante @FXML.
 *
 * Flujo principal:
 *   1. Usuario rellena nombre, apellidos, teléfono, comensales, fecha, hora, mesa
 *   2. Al seleccionar fecha + turno → mapa de sala se actualiza en tiempo real
 *   3. Al confirmar → se busca o crea el cliente, se crea la reserva en BD
 */
public class ReservaControlador {

    // ── Componentes FXML ─────────────────────────────────────────
    @FXML private TextField          txtNombre;
    @FXML private TextField          txtApellidos;
    @FXML private TextField          txtTelefono;
    @FXML private TextField          txtMesaSeleccionada;  // solo lectura, se rellena al pulsar una mesa
    @FXML private Spinner<Integer>   spinnerComensales;
    @FXML private DatePicker         fechaReserva;
    @FXML private ComboBox<String>   comboHora;
    @FXML private ComboBox<String>   comboAlergias;
    @FXML private TextArea           txtObservaciones;
    @FXML private GridPane           gridMapa;

    // ── DAOs ─────────────────────────────────────────────────────
    private ClienteDAO clienteDAO;
    private ReservaDAO reservaDAO;

    // ── Estado interno ───────────────────────────────────────────
    // id de la mesa seleccionada en el mapa (se resuelve desde ReservaDAO)
    private int idMesaSeleccionada = -1;

    // Franjas horarias fijas por turno
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
        configurarComboAlergias();
        configurarMapa();

        // Actualizar horas y sala cuando cambia fecha o turno
        fechaReserva.valueProperty().addListener((o, old, v) -> {
            actualizarComboHoras(v);
            actualizarSala();
        });
        comboHora.valueProperty().addListener((o, old, v) -> actualizarSala());
    }

    // ════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE COMPONENTES
    // ════════════════════════════════════════════════════════════

    /** Solo números, máximo 9 dígitos (formato español) */
    private void configurarTelefono() {
        txtTelefono.textProperty().addListener((o, old, v) -> {
            if (!v.matches("\\d*") || v.length() > 9) txtTelefono.setText(old);
        });
    }

    /**
     * StringConverter personalizado para evitar el NullPointerException
     * que lanza el Spinner cuando el usuario borra el contenido del campo.
     * Muestra vacío cuando el valor es 0, acepta cualquier entrada numérica.
     */
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
                if (s == null || s.trim().isEmpty()) return 0;
                try { return Integer.parseInt(s.trim()); }
                catch (NumberFormatException e) { return 0; }
            }
        });

        spinnerComensales.setValueFactory(factory);
        spinnerComensales.setEditable(true);
    }

    /** Bloquea fechas pasadas en el DatePicker */
    private void configurarCalendario() {
        fechaReserva.setDayCellFactory(picker -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (date.isBefore(LocalDate.now())) {
                    setDisable(true);
                    setStyle("-fx-background-color: #2a2a3b;");
                }
            }
        });
    }

    /**
     * Carga el catálogo de intolerancias desde la BD y construye el ComboBox
     * con dos secciones separadas por cabeceras no seleccionables:
     *   → "14 ALÉRGENOS OFICIALES (UE)"
     *   → "INTOLERANCIAS COMUNES"
     */
    private void configurarComboAlergias() {
        List<Intolerancia> catalogo = clienteDAO.obtenerCatalogoIntolerancias();
        ObservableList<String> items = FXCollections.observableArrayList();

        items.add("--- NINGUNA ---");
        items.add("--- 14 ALÉRGENOS OFICIALES (UE) ---");

        // Los primeros 14 son ALERGENO_UE (el SQL los devuelve ordenados así)
        catalogo.stream()
                .filter(i -> "ALERGENO_UE".equals(i.getTipo()))
                .forEach(i -> items.add(i.getNombre()));

        items.add("--- INTOLERANCIAS COMUNES ---");

        catalogo.stream()
                .filter(i -> "INTOLERANCIA".equals(i.getTipo()))
                .forEach(i -> items.add(i.getNombre()));

        comboAlergias.setItems(items);

        // Impedir que el usuario seleccione una cabecera como valor real
        comboAlergias.valueProperty().addListener((o, old, v) -> {
            if (v != null && v.startsWith("---") && !v.equals("--- NINGUNA ---")) {
                comboAlergias.setValue(old);
            }
        });
    }

    /**
     * Asigna la acción de selección a cada botón del mapa.
     * Al pulsar un botón: se marca visualmente y se guarda su número
     * en txtMesaSeleccionada para que el controlador lo lea al confirmar.
     */
    private void configurarMapa() {
        for (Node node : gridMapa.getChildren()) {
            if (node instanceof Button btn) {
                btn.setOnAction(e -> seleccionarMesa(btn));
            }
        }
    }

    private void seleccionarMesa(Button btn) {
        // Quitar selección anterior
        gridMapa.getChildren().forEach(n -> {
            if (n instanceof Button b) b.getStyleClass().remove("mesa-reservada");
        });
        // Marcar la nueva selección
        btn.getStyleClass().add("mesa-reservada");
        txtMesaSeleccionada.setText(btn.getText());

        // Resolver el id_mesa desde la BD para usarlo al crear la reserva
        idMesaSeleccionada = reservaDAO.resolverIdMesa(btn.getText());
    }

    // ════════════════════════════════════════════════════════════
    // LÓGICA DE SALA
    // ════════════════════════════════════════════════════════════

    /**
     * Filtra las horas del ComboBox según el turno y la hora actual.
     * Si la fecha es hoy, elimina las horas que ya han pasado
     * (con un margen de 15 minutos para no crear reservas imposibles).
     */
    private void actualizarComboHoras(LocalDate fecha) {
        if (fecha == null) return;
        LocalTime margen = LocalTime.now().plusMinutes(15);
        ObservableList<String> items = FXCollections.observableArrayList();

        List<String> comida = HORAS_COMIDA.stream()
                .filter(h -> !fecha.equals(LocalDate.now()) || LocalTime.parse(h).isAfter(margen))
                .collect(Collectors.toList());

        List<String> cena = HORAS_CENA.stream()
                .filter(h -> !fecha.equals(LocalDate.now()) || LocalTime.parse(h).isAfter(margen))
                .collect(Collectors.toList());

        if (!comida.isEmpty()) { items.add("--- TURNO COMIDA ---"); items.addAll(comida); }
        if (!cena.isEmpty())   { items.add("--- TURNO CENA ---");   items.addAll(cena); }

        comboHora.setItems(items);
    }

    /**
     * Actualiza el estado visual del mapa de sala consultando la BD.
     * Solo se ejecuta si hay fecha y turno seleccionados.
     *
     * Estados visuales:
     *   mesa-libre    → verde, seleccionable
     *   mesa-ocupada  → rojo/gris, deshabilitada (opacidad 0.5)
     */
    private void actualizarSala() {
        boolean datosCompletos = fechaReserva.getValue() != null
                && comboHora.getValue() != null
                && !comboHora.getValue().startsWith("---");

        gridMapa.setVisible(datosCompletos);
        if (!datosCompletos) return;

        // Obtener mesas ocupadas para esta fecha y turno desde ReservaDAO
        List<String> ocupadas = reservaDAO.obtenerMesasOcupadas(
                fechaReserva.getValue(),
                comboHora.getValue()
        );

        for (Node node : gridMapa.getChildren()) {
            if (node instanceof Button btn) {
                // Reset completo antes de aplicar el nuevo estado
                btn.getStyleClass().removeAll("mesa-libre", "mesa-ocupada", "mesa-reservada");
                btn.setDisable(false);
                btn.setOpacity(1.0);

                if (ocupadas.contains(btn.getText())) {
                    btn.getStyleClass().add("mesa-ocupada");
                    btn.setDisable(true);
                    btn.setOpacity(0.5);
                } else {
                    btn.getStyleClass().add("mesa-libre");
                }
            }
        }
    }

    // ════════════════════════════════════════════════════════════
    // ACCIONES DE BOTONES
    // ════════════════════════════════════════════════════════════

    /**
     * Valida el formulario, busca o crea el cliente y registra la reserva.
     *
     * Flujo:
     *   1. Validar campos obligatorios
     *   2. Buscar cliente por teléfono → si no existe, crearlo
     *   3. Crear la reserva en la tabla reservas vía ReservaDAO
     *   4. Actualizar el mapa de sala
     */
    @FXML
    public void onConfirmarClick() {
        if (!validarFormulario()) return;

        // ── 1. Buscar o crear cliente ────────────────────────────
        Cliente cliente = clienteDAO.buscarPorTelefono(txtTelefono.getText());

        if (cliente == null) {
            // Cliente nuevo: registrarlo y obtener su id
            cliente = new Cliente(
                    txtNombre.getText() + " " + txtApellidos.getText(),
                    txtTelefono.getText(),
                    null,           // email: no se pide en este formulario
                    null            // observaciones: se añaden desde la vista de gestión
            );
            int idNuevo = clienteDAO.registrarCliente(cliente);
            if (idNuevo == -1) {
                mostrarAlerta("Error", "No se pudo registrar el cliente. Comprueba la conexión.");
                return;
            }
            cliente.setIdCliente(idNuevo);
        }

        // ── 2. Construir la reserva ──────────────────────────────
        String alergia = (comboAlergias.getValue() == null
                || comboAlergias.getValue().startsWith("---"))
                ? null
                : comboAlergias.getValue();

        // El número de comensales mínimo es 1 aunque el spinner muestre vacío
        int comensales = (spinnerComensales.getValue() == 0) ? 1 : spinnerComensales.getValue();

        Reserva reserva = new Reserva(
                cliente.getIdCliente(),
                idMesaSeleccionada,
                fechaReserva.getValue(),
                LocalTime.parse(comboHora.getValue()),
                comensales,
                (alergia != null ? "Alergia: " + alergia + ". " : "")
                        + txtObservaciones.getText()
        );

        // ── 3. Guardar la reserva en BD ──────────────────────────
        int idReserva = reservaDAO.crearReserva(reserva);
        if (idReserva == -1) {
            mostrarAlerta("Error", "No se pudo guardar la reserva. Comprueba la conexión.");
            return;
        }

        mostrarAlerta("Reserva confirmada",
                "Reserva registrada correctamente para " + cliente.getNombre() + ".");
        onLimpiarClick();
        actualizarSala();
    }

    @FXML
    public void onLimpiarClick() {
        txtNombre.clear();
        txtApellidos.clear();
        txtTelefono.clear();
        txtMesaSeleccionada.clear();
        txtObservaciones.clear();
        fechaReserva.setValue(null);
        comboHora.setValue(null);
        comboAlergias.setValue(null);
        spinnerComensales.getValueFactory().setValue(0);
        gridMapa.setVisible(false);
        idMesaSeleccionada = -1;
    }

    // ════════════════════════════════════════════════════════════
    // UTILIDADES
    // ════════════════════════════════════════════════════════════

    /**
     * Comprueba que todos los campos obligatorios estén rellenos.
     * Devuelve false y muestra alerta si falta alguno.
     */
    private boolean validarFormulario() {
        if (txtNombre.getText().isBlank()) {
            mostrarAlerta("Campo requerido", "Introduce el nombre del cliente.");
            return false;
        }
        if (txtApellidos.getText().isBlank()) {
            mostrarAlerta("Campo requerido", "Introduce los apellidos del cliente.");
            return false;
        }
        if (txtTelefono.getText().isBlank()) {
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
        if (txtMesaSeleccionada.getText().isBlank() || idMesaSeleccionada == -1) {
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
