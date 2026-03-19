package datos;

import modelo.Reserva;
import modelo.ReservaView;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la tabla 'reservas'.
 *
 * Responsabilidades:
 *   - Crear, consultar, modificar y cancelar reservas
 *   - Consultar disponibilidad de mesas por fecha y turno
 *   - Resolver el id_mesa a partir del número de mesa (M1-M12)
 *
 * Sustituye completamente la lógica provisional que usaba el campo
 * 'observaciones' de clientes para almacenar datos de reservas.
 */
public class ReservaDAO {

    private final ConexionConMySQL conexion;

    public ReservaDAO() {
        this.conexion = new ConexionConMySQL();
    }

    // ════════════════════════════════════════════════════════════
    // CREAR
    // ════════════════════════════════════════════════════════════

    /**
     * Inserta una nueva reserva en la BD.
     * Devuelve el id_reserva generado, o -1 si falla.
     */
    public int crearReserva(Reserva reserva) {
        String sql = "INSERT INTO reservas " +
                     "(id_cliente, id_mesa, fecha_reserva, hora_reserva, " +
                     " comensales, estado_reserva, observaciones) " +
                     "VALUES (?, ?, ?, ?, ?, 'CONFIRMADA', ?)";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, reserva.getIdCliente());
            stmt.setInt(2, reserva.getIdMesa());
            stmt.setDate(3, Date.valueOf(reserva.getFechaReserva()));
            stmt.setTime(4, Time.valueOf(reserva.getHoraReserva()));
            stmt.setInt(5, reserva.getComensales());
            stmt.setString(6, reserva.getObservaciones());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error al crear reserva: " + e.getMessage());
        }
        return -1;
    }

    // ════════════════════════════════════════════════════════════
    // CONSULTAR
    // ════════════════════════════════════════════════════════════

    /**
     * Devuelve todas las reservas de una fecha concreta.
     * Hace JOIN con clientes y mesas para obtener los nombres
     * directamente en el objeto Reserva (para el TableView).
     */
    public List<Reserva> obtenerPorFecha(LocalDate fecha) {
        List<Reserva> lista = new ArrayList<>();
        String sql = "SELECT r.id_reserva, r.id_cliente, r.id_mesa, " +
                     "       r.fecha_reserva, r.hora_reserva, r.comensales, " +
                     "       r.estado_reserva, r.observaciones, " +
                     "       c.nombre_completo, m.numero_mesa " +
                     "FROM reservas r " +
                     "JOIN clientes c ON r.id_cliente = c.id_cliente " +
                     "JOIN mesas    m ON r.id_mesa    = m.id_mesa " +
                     "WHERE r.fecha_reserva = ? " +
                     "ORDER BY r.hora_reserva";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(fecha));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener reservas por fecha: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Devuelve todas las reservas activas (CONFIRMADA) de un cliente.
     * Se usa en la vista de gestión al buscar por cliente.
     */
    public List<Reserva> obtenerPorCliente(int idCliente) {
        List<Reserva> lista = new ArrayList<>();
        String sql = "SELECT r.id_reserva, r.id_cliente, r.id_mesa, " +
                     "       r.fecha_reserva, r.hora_reserva, r.comensales, " +
                     "       r.estado_reserva, r.observaciones, " +
                     "       c.nombre_completo, m.numero_mesa " +
                     "FROM reservas r " +
                     "JOIN clientes c ON r.id_cliente = c.id_cliente " +
                     "JOIN mesas    m ON r.id_mesa    = m.id_mesa " +
                     "WHERE r.id_cliente = ? " +
                     "ORDER BY r.fecha_reserva, r.hora_reserva";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idCliente);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener reservas por cliente: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Devuelve los números de mesa (ej: "M1", "M3") que ya tienen
     * una reserva CONFIRMADA para la fecha y hora indicadas.
     *
     * Lo usa el controlador para pintar el mapa de sala en tiempo real.
     * La hora se compara exacta porque cada turno tiene franjas fijas.
     */
    public List<String> obtenerMesasOcupadas(LocalDate fecha, String hora) {
        List<String> mesas = new ArrayList<>();
        String sql = "SELECT m.numero_mesa " +
                     "FROM reservas r " +
                     "JOIN mesas m ON r.id_mesa = m.id_mesa " +
                     "WHERE r.fecha_reserva = ? " +
                     "  AND r.hora_reserva  = ? " +
                     "  AND r.estado_reserva = 'CONFIRMADA'";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDate(1, Date.valueOf(fecha));
            stmt.setTime(2, Time.valueOf(LocalTime.parse(hora)));

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) mesas.add(rs.getString("numero_mesa"));
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener mesas ocupadas: " + e.getMessage());
        }
        return mesas;
    }

    /**
     * Comprueba si una mesa concreta está disponible para una fecha y hora.
     * Devuelve true si NO hay ninguna reserva CONFIRMADA que la bloquee.
     */
    public boolean estaDisponible(int idMesa, LocalDate fecha, String hora) {
        String sql = "SELECT COUNT(*) FROM reservas " +
                     "WHERE id_mesa = ? " +
                     "  AND fecha_reserva  = ? " +
                     "  AND hora_reserva   = ? " +
                     "  AND estado_reserva = 'CONFIRMADA'";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idMesa);
            stmt.setDate(2, Date.valueOf(fecha));
            stmt.setTime(3, Time.valueOf(LocalTime.parse(hora)));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) == 0;
            }

        } catch (SQLException e) {
            System.err.println("Error al comprobar disponibilidad: " + e.getMessage());
        }
        return false;
    }

    // ════════════════════════════════════════════════════════════
    // MODIFICAR / CANCELAR
    // ════════════════════════════════════════════════════════════

    /**
     * Cambia el estado de una reserva.
     * Valores válidos: "CONFIRMADA", "CANCELADA", "FINALIZADA"
     */
    public boolean cambiarEstado(int idReserva, String nuevoEstado) {
        String sql = "UPDATE reservas SET estado_reserva = ? WHERE id_reserva = ?";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, idReserva);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al cambiar estado de reserva: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza los datos editables de una reserva existente:
     * mesa, fecha, hora, comensales y observaciones.
     */
    public boolean actualizarReserva(Reserva reserva) {
        String sql = "UPDATE reservas SET " +
                     "id_mesa = ?, fecha_reserva = ?, hora_reserva = ?, " +
                     "comensales = ?, observaciones = ? " +
                     "WHERE id_reserva = ?";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, reserva.getIdMesa());
            stmt.setDate(2, Date.valueOf(reserva.getFechaReserva()));
            stmt.setTime(3, Time.valueOf(reserva.getHoraReserva()));
            stmt.setInt(4, reserva.getComensales());
            stmt.setString(5, reserva.getObservaciones());
            stmt.setInt(6, reserva.getIdReserva());
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al actualizar reserva: " + e.getMessage());
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    // RESOLUCIÓN DE IDs
    // ════════════════════════════════════════════════════════════

    /**
     * Traduce el texto de un botón del mapa ("M1", "M12"...)
     * al id_mesa correspondiente en la base de datos.
     *
     * Resuelve la discrepancia entre los identificadores visuales
     * del FXML y los registros de la tabla mesas.
     * Devuelve -1 si no se encuentra la mesa.
     */
    public int resolverIdMesa(String numeroMesa) {
        String sql = "SELECT id_mesa FROM mesas WHERE numero_mesa = ?";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, numeroMesa);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id_mesa");
            }

        } catch (SQLException e) {
            System.err.println("Error al resolver id_mesa: " + e.getMessage());
        }
        return -1;
    }

    // ════════════════════════════════════════════════════════════
    // UTILIDADES PRIVADAS
    // ════════════════════════════════════════════════════════════

    /** Construye un objeto Reserva a partir del ResultSet actual */
    private Reserva mapear(ResultSet rs) throws SQLException {
        Reserva r = new Reserva();
        r.setIdReserva(rs.getInt("id_reserva"));
        r.setIdCliente(rs.getInt("id_cliente"));
        r.setIdMesa(rs.getInt("id_mesa"));
        r.setFechaReserva(rs.getDate("fecha_reserva").toLocalDate());
        r.setHoraReserva(rs.getTime("hora_reserva").toLocalTime());
        r.setComensales(rs.getInt("comensales"));
        r.setEstadoReserva(rs.getString("estado_reserva"));
        r.setObservaciones(rs.getString("observaciones"));
        r.setNombreCliente(rs.getString("nombre_completo"));
        r.setNumeroMesa(rs.getString("numero_mesa"));
        return r;
    }

    public boolean cancelarReserva(int idReserva) {
        return false;
    }

    public List<ReservaView> listarReservasView() {
        return List.of();
    }

    public List<ReservaView> buscarReservas(String texto, String fecha) {
        return List.of();
    }

    public Reserva obtenerReservaPorId(int idReserva) {
        return null;
    }
}
