package datos;

import modelo.PreautorizacionPago;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;

/**
 * DAO para la tabla 'preautorizaciones_pago'.
 *
 * Gestiona el ciclo de vida completo de una preautorización:
 *   PENDIENTE → AUTORIZADA → DEVUELTA  (reserva completada)
 *   PENDIENTE → RECHAZADA              (simulación de pago denegado)
 *   PENDIENTE/AUTORIZADA → DEVUELTA    (cancelación o no-show)
 *
 * No integra pasarelas reales. Ver memoria, apartado 2.3.2.
 */
public class PagoDAO {

    private final ConexionConMySQL conexion;

    public PagoDAO() {
        this.conexion = new ConexionConMySQL();
    }

    /**
     * Crea una preautorización nueva al confirmar una reserva.
     * El importe se calcula en el controlador (por ejemplo: 10€ por comensal).
     * Devuelve el id_pago generado, o -1 si falla.
     */
    public int crearPreautorizacion(int idReserva, BigDecimal importe) {
        String sql = "INSERT INTO preautorizaciones_pago (id_reserva, importe, estado) " +
                     "VALUES (?, ?, 'PENDIENTE')";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, idReserva);
            stmt.setBigDecimal(2, importe);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error al crear preautorización: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Busca la preautorización asociada a una reserva concreta.
     * Devuelve null si no existe o hay error.
     */
    public PreautorizacionPago obtenerPorReserva(int idReserva) {
        String sql = "SELECT id_pago, id_reserva, importe, estado, " +
                     "       fecha_creacion, fecha_gestion " +
                     "FROM preautorizaciones_pago WHERE id_reserva = ?";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idReserva);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener preautorización: " + e.getMessage());
        }
        return null;
    }

    /**
     * Cambia el estado de una preautorización y registra la fecha de gestión.
     * Valores válidos: "AUTORIZADA", "RECHAZADA", "DEVUELTA"
     *
     * La fecha_gestion se establece automáticamente con NOW() en MySQL,
     * lo que garantiza que el timestamp es el del servidor, no del cliente.
     */
    public boolean cambiarEstado(int idPago, String nuevoEstado) {
        String sql = "UPDATE preautorizaciones_pago " +
                     "SET estado = ?, fecha_gestion = NOW() " +
                     "WHERE id_pago = ?";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, idPago);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al cambiar estado de preautorización: " + e.getMessage());
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    // UTILIDADES PRIVADAS
    // ════════════════════════════════════════════════════════════

    private PreautorizacionPago mapear(ResultSet rs) throws SQLException {
        PreautorizacionPago p = new PreautorizacionPago();
        p.setIdPago(rs.getInt("id_pago"));
        p.setIdReserva(rs.getInt("id_reserva"));
        p.setImporte(rs.getBigDecimal("importe"));
        p.setEstado(rs.getString("estado"));

        Timestamp tc = rs.getTimestamp("fecha_creacion");
        if (tc != null) p.setFechaCreacion(tc.toLocalDateTime());

        Timestamp tg = rs.getTimestamp("fecha_gestion");
        if (tg != null) p.setFechaGestion(tg.toLocalDateTime());

        return p;
    }
}
