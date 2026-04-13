package com.nexervo.controlador;

import com.nexervo.datos.EstadisticasDAO;
import com.nexervo.modelo.Usuario;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.Label;

import java.util.Map;

/**
 * Controlador del panel de estadísticas (Dashboard).
 *
 * Este panel solo lo ve el administrador (gerente / maître jefe).
 * Las métricas que puse son las que yo mismo consultaba de manera informal
 * cuando trabajaba: qué días había más lío, a qué hora se llenaba el comedor,
 * qué clientes venían más y cuánto facturábamos al mes.
 *
 * Lo más complicado fue darle color a las barras de las gráficas.
 * JavaFX Charts no permite cambiar el color directamente desde FXML (al menos
 * en la versión 21 que uso), así que tuve que buscar cómo hacerlo por código.
 * Al final encontré que con lookupAll(".bar") se puede acceder a los nodos
 * de las barras después de renderizarlas y cambiarles el estilo.
 * No es lo más elegante del mundo pero funciona.
 */
public class DashboardControlador implements PrincipalControlador.NecesitaUsuario {

    // KPI labels
    @FXML private Label lblKpiHoy;
    @FXML private Label lblKpiMes;
    @FXML private Label lblKpiClientes;
    @FXML private Label lblKpiAusencias;

    // Gráficas
    @FXML private BarChart<String, Number>  chartDias;
    @FXML private BarChart<String, Number>  chartFranjas;
    @FXML private BarChart<String, Number>  chartClientes;
    @FXML private LineChart<String, Number> chartIngresos;

    private final EstadisticasDAO estadDAO = new EstadisticasDAO();

    @Override
    public void setUsuario(Usuario u) {
        // No necesito el usuario aquí porque el acceso ya lo controla PrincipalControlador
        // (el botón de estadísticas solo aparece si eres ADMIN)
    }

    @FXML
    public void initialize() {
        cargarDatos();
    }

    @FXML
    public void onActualizar() {
        // El refresco automático queda fuera del alcance de esta versión;
        // el botón manual es suficiente para el volumen de un restaurante.
        limpiarGraficas();
        cargarDatos();
    }

    // ── Carga completa ────────────────────────────────────────────

    private void cargarDatos() {
        cargarKpis();
        cargarDiaSemana();
        cargarFranjas();
        cargarTopClientes();
        cargarIngresosMes();
    }

    private void limpiarGraficas() {
        chartDias.getData().clear();
        chartFranjas.getData().clear();
        chartClientes.getData().clear();
        chartIngresos.getData().clear();
    }

    // ── KPIs ──────────────────────────────────────────────────────

    private void cargarKpis() {
        lblKpiHoy.setText(String.valueOf(estadDAO.reservasHoy()));
        lblKpiMes.setText(String.valueOf(estadDAO.reservasMes()));
        lblKpiClientes.setText(String.valueOf(estadDAO.totalClientes()));
        int ausencias = estadDAO.ausenciasMes();
        lblKpiAusencias.setText(String.valueOf(ausencias));
        // Resaltar si hay no se presentós
        lblKpiAusencias.setStyle(ausencias > 0
            ? "-fx-text-fill: #FF6688; -fx-font-size: 32px; -fx-font-weight: bold;"
            : "-fx-text-fill: #00E676; -fx-font-size: 32px; -fx-font-weight: bold;");
    }

    // ── Días de la semana ─────────────────────────────────────────

    private void cargarDiaSemana() {
        Map<String, Integer> datos = estadDAO.reservasPorDiaSemana();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        datos.forEach((dia, total) -> serie.getData().add(new XYChart.Data<>(dia, total)));
        chartDias.getData().add(serie);
        // Aplicar color a las barras después de añadirlas
        aplicarEstiloBarras(chartDias, "#5D3FD3");
    }

    // ── Franjas horarias ──────────────────────────────────────────

    private void cargarFranjas() {
        Map<String, Integer> datos = estadDAO.reservasPorFranja();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        datos.forEach((hora, total) -> serie.getData().add(new XYChart.Data<>(hora, total)));
        chartFranjas.getData().add(serie);
        aplicarEstiloBarras(chartFranjas, "#00C6FF");
    }

    // ── Top 5 clientes ────────────────────────────────────────────

    private void cargarTopClientes() {
        Map<String, Integer> datos = estadDAO.top5Clientes();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        datos.forEach((nombre, visitas) -> {
            // Acortar nombre si es muy largo para que quepa en el eje
            String etiqueta = nombre.length() > 14 ? nombre.substring(0, 13) + "…" : nombre;
            serie.getData().add(new XYChart.Data<>(etiqueta, visitas));
        });
        chartClientes.getData().add(serie);
        aplicarEstiloBarras(chartClientes, "#FFC107");
    }

    // ── Ingresos por mes ──────────────────────────────────────────

    private void cargarIngresosMes() {
        Map<String, Double> datos = estadDAO.ingresosPorMes();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        datos.forEach((mes, importe) -> serie.getData().add(new XYChart.Data<>(mes, importe)));
        chartIngresos.getData().add(serie);
        // Estilo de la línea
        chartIngresos.lookupAll(".chart-series-line").forEach(n ->
            n.setStyle("-fx-stroke: #00C6FF; -fx-stroke-width: 2.5px;"));
        chartIngresos.lookupAll(".chart-line-symbol").forEach(n ->
            n.setStyle("-fx-background-color: #00C6FF, white; -fx-background-radius: 4;"));
    }

    // ── Utilidades de estilo ─────────────────────────────────────

    /**
     * Aplica color a las barras de un BarChart.
     *
     * JavaFX no permite cambiar el color de las barras directamente desde FXML
     * cuando usas series — tienes que hacer lookup del CSS después de que el
     * gráfico se haya renderizado. Lo encontré en el foro de StackOverflow y
     * en la documentación de JavaFX CSS Reference Guide.
     *
     * El ".chart-plot-background" es para oscurecer el fondo del gráfico y que
     * encaje con el tema oscuro del resto de la app.
     */
    private void aplicarEstiloBarras(BarChart<?, ?> chart, String color) {
        chart.lookupAll(".bar").forEach(n ->
            n.setStyle("-fx-bar-fill: " + color + ";"));
        chart.lookupAll(".chart-plot-background").forEach(n ->
            n.setStyle("-fx-background-color: rgba(15,10,50,0.4);"));
    }
}
