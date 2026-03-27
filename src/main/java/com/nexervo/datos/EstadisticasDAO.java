package com.nexervo.datos;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DAO solo para leer estadísticas — no hace ningún INSERT ni UPDATE.
 *
 * Decidí separarlo del ReservaDAO para que quedara más claro.
 * Mi tutor me dijo que era buena práctica separar las responsabilidades
 * y la verdad es que tiene sentido: las consultas de este DAO son todas
 * agregaciones (COUNT, SUM, GROUP BY) y no tiene sentido mezclarlas
 * con el CRUD normal.
 *
 * Las métricas las elegí a partir de lo que yo mismo miraba cuando trabajaba
 * de maitre: días pico de la semana, franjas horarias más ocupadas, y quiénes
 * son los clientes habituales para saludarlos por su nombre.
 */
public class EstadisticasDAO {

    private static final Logger log = LoggerFactory.getLogger(EstadisticasDAO.class);

    private final ConexionBD conexion;

    // Nombres de días para el eje X del gráfico
    private static final String[] DIAS = {"Lun","Mar","Mié","Jue","Vie","Sáb","Dom"};

    public EstadisticasDAO() {
        this.conexion = new ConexionConMySQL();
    }

    // ────────────────────────────────────────────────────────────
    // Reservas por día de la semana (últimos 90 días, excluyendo canceladas)
    // ────────────────────────────────────────────────────────────
    public Map<String, Integer> reservasPorDiaSemana() {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        for (String d : DIAS) mapa.put(d, 0);

        // ATENCIÓN: DAYOFWEEK en MySQL devuelve 1=domingo, 2=lunes ... 7=sábado
        // (no como en Java que empieza en lunes).
        // Me costó un rato entender por qué el gráfico salía desfasado un día — esto lo arregla.
        String sql = "SELECT DAYOFWEEK(fecha_reserva) AS dia, COUNT(*) AS total " +
                     "FROM reservas " +
                     "WHERE estado_reserva NOT IN ('CANCELADA') " +
                     "  AND fecha_reserva >= CURDATE() - INTERVAL 90 DAY " +
                     "GROUP BY dia ORDER BY dia";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int diaMysql = rs.getInt("dia"); // 1=dom, 2=lun...
                int idx = (diaMysql == 1) ? 6 : diaMysql - 2; // convertir a 0=lun...6=dom
                if (idx >= 0 && idx < DIAS.length) {
                    mapa.put(DIAS[idx], rs.getInt("total"));
                }
            }
        } catch (SQLException e) {
            log.error("Error al obtener estadísticas por día", e);
        }
        return mapa;
    }

    // ────────────────────────────────────────────────────────────
    // Reservas por franja horaria (últimos 90 días)
    // ────────────────────────────────────────────────────────────
    public Map<String, Integer> reservasPorFranja() {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        String sql = "SELECT TIME_FORMAT(hora_reserva, '%H:%i') AS hora, COUNT(*) AS total " +
                     "FROM reservas " +
                     "WHERE estado_reserva NOT IN ('CANCELADA') " +
                     "  AND fecha_reserva >= CURDATE() - INTERVAL 90 DAY " +
                     "GROUP BY hora ORDER BY hora";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) mapa.put(rs.getString("hora"), rs.getInt("total"));

        } catch (SQLException e) {
            log.error("Error al obtener estadísticas por franja horaria", e);
        }
        return mapa;
    }

    // ────────────────────────────────────────────────────────────
    // Top 5 clientes más frecuentes (por visitas completadas)
    // ────────────────────────────────────────────────────────────
    public Map<String, Integer> top5Clientes() {
        Map<String, Integer> mapa = new LinkedHashMap<>();
        String sql = "SELECT c.nombre_completo, COUNT(*) AS visitas " +
                     "FROM reservas r " +
                     "JOIN clientes c ON r.id_cliente = c.id_cliente " +
                     "WHERE r.estado_reserva = 'FINALIZADA' " +
                     "GROUP BY r.id_cliente, c.nombre_completo " +
                     "ORDER BY visitas DESC LIMIT 5";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) mapa.put(rs.getString("nombre_completo"), rs.getInt("visitas"));

        } catch (SQLException e) {
            log.error("Error al obtener top 5 clientes", e);
        }
        return mapa;
    }

    // ────────────────────────────────────────────────────────────
    // Ingresos estimados por mes (año en curso, reservas finalizadas)
    // ────────────────────────────────────────────────────────────
    public Map<String, Double> ingresosPorMes() {
        Map<String, Double> mapa = new LinkedHashMap<>();
        String[] meses = {"Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"};
        for (String m : meses) mapa.put(m, 0.0);

        // Estimación: precio medio por comensal = 10 €, valor razonable para un
        // restaurante de precio medio en España. Sin TPV integrado, esta aproximación
        // es suficiente para ofrecer una tendencia de ingresos al gestor.
        String sql = "SELECT MONTH(fecha_reserva) AS mes, " +
                     "       SUM(comensales * 10.00) AS total " +
                     "FROM reservas " +
                     "WHERE estado_reserva = 'FINALIZADA' " +
                     "  AND YEAR(fecha_reserva) = YEAR(CURDATE()) " +
                     "GROUP BY mes ORDER BY mes";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int idx = rs.getInt("mes") - 1; // 1-12 → 0-11
                if (idx >= 0 && idx < meses.length) mapa.put(meses[idx], rs.getDouble("total"));
            }
        } catch (SQLException e) {
            log.error("Error al calcular ingresos por mes", e);
        }
        return mapa;
    }

    // ────────────────────────────────────────────────────────────
    // KPIs rápidos para el encabezado del dashboard
    // ────────────────────────────────────────────────────────────

    /** Total de reservas confirmadas para hoy */
    public int reservasHoy() {
        String sql = "SELECT COUNT(*) FROM reservas WHERE fecha_reserva = CURDATE() " +
                     "AND estado_reserva = 'CONFIRMADA'";
        return contarUno(sql);
    }

    /** Total de clientes registrados */
    public int totalClientes() {
        return contarUno("SELECT COUNT(*) FROM clientes");
    }

    /** No-shows del mes en curso */
    public int ausenciasMes() {
        String sql = "SELECT COUNT(*) FROM reservas WHERE estado_reserva = 'NO_PRESENTADO' " +
                     "AND MONTH(fecha_reserva) = MONTH(CURDATE()) " +
                     "AND YEAR(fecha_reserva) = YEAR(CURDATE())";
        return contarUno(sql);
    }

    /** Reservas totales del mes en curso (sin canceladas) */
    public int reservasMes() {
        String sql = "SELECT COUNT(*) FROM reservas WHERE estado_reserva != 'CANCELADA' " +
                     "AND MONTH(fecha_reserva) = MONTH(CURDATE()) " +
                     "AND YEAR(fecha_reserva) = YEAR(CURDATE())";
        return contarUno(sql);
    }

    // ── helper ──
    private int contarUno(String sql) {
        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            log.error("Error al obtener KPI", e);
        }
        return 0;
    }
}
