package com.nexervo.datos;

import com.nexervo.modelo.Reserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la tabla 'reservas'.
 *
 * Aquí van todas las operaciones CRUD sobre reservas.
 * v3: añadí los campos 'ocasion' y 'peticiones_especiales' y el estado NO_PRESENTADO.
 *
 * Nota importante: el orden de los campos en los INSERT y UPDATE tiene que
 * coincidir exactamente con los setInt/setString del PreparedStatement.
 * Me olvidé una vez y estuve un buen rato buscando por qué los datos salían
 * cruzados en la BD — lo dejo comentado para no repetirlo.
 */
public class ReservaDAO {

    private static final Logger log = LoggerFactory.getLogger(ReservaDAO.class);
    private final ConexionBD conexion;

    public ReservaDAO() {
        this.conexion = new ConexionConMySQL();
    }

    /** Constructor para tests: permite inyectar cualquier ConexionBD (ej: H2). */
    public ReservaDAO(ConexionBD conexion) {
        this.conexion = conexion;
    }

    // ════════════════════════════════════════════════════════════
    // CREAR
    // ════════════════════════════════════════════════════════════

    /** Inserta una nueva reserva. Devuelve el id generado, o -1 si falla. */
    public int crearReserva(Reserva reserva) {
        String sql = "INSERT INTO reservas " +
                     "(id_cliente, id_mesa, fecha_reserva, hora_reserva, comensales, " +
                     " estado_reserva, ocasion, peticiones_especiales, observaciones) " +
                     "VALUES (?, ?, ?, ?, ?, 'CONFIRMADA', ?, ?, ?)";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, reserva.getIdCliente());
            stmt.setInt(2, reserva.getIdMesa());
            stmt.setDate(3, Date.valueOf(reserva.getFechaReserva()));
            stmt.setTime(4, Time.valueOf(reserva.getHoraReserva()));
            stmt.setInt(5, reserva.getComensales());
            stmt.setString(6, reserva.getOcasion());
            stmt.setString(7, reserva.getPeticionesEspeciales());
            stmt.setString(8, reserva.getObservaciones());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    int idGenerado = keys.getInt(1);
                    // System.out.println("DEBUG crearReserva: id generado = " + idGenerado);
                    return idGenerado;
                }
            }

        } catch (SQLException e) {
            log.error("Error al crear reserva", e);
        }
        return -1;
    }

    // ════════════════════════════════════════════════════════════
    // CONSULTAR
    // ════════════════════════════════════════════════════════════

    /** Reservas de una fecha concreta, con JOIN de cliente y mesa.
     *  Para el volumen de un restaurante no se requiere paginación. */
    public List<Reserva> obtenerPorFecha(LocalDate fecha) {
        List<Reserva> lista = new ArrayList<>();
        String sql = "SELECT r.*, c.nombre_completo, m.numero_mesa " +
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
            log.error("Error al obtener reservas por fecha", e);
        }
        return lista;
    }

    /** Todas las reservas de un cliente (historial completo, más reciente primero). */
    public List<Reserva> obtenerPorCliente(int idCliente) {
        List<Reserva> lista = new ArrayList<>();
        String sql = "SELECT r.*, c.nombre_completo, m.numero_mesa " +
                     "FROM reservas r " +
                     "JOIN clientes c ON r.id_cliente = c.id_cliente " +
                     "JOIN mesas    m ON r.id_mesa    = m.id_mesa " +
                     "WHERE r.id_cliente = ? " +
                     "ORDER BY r.fecha_reserva DESC, r.hora_reserva";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idCliente);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            log.error("Error al obtener reservas por cliente", e);
        }
        return lista;
    }

    /**
     * Números de mesa con reserva CONFIRMADA para una fecha y hora exactas.
     * Usado por el mapa de sala para pintar mesas en tiempo real.
     */
    public List<String> obtenerMesasOcupadas(LocalDate fecha, String hora) {
        List<String> mesas = new ArrayList<>();
        String sql = "SELECT m.numero_mesa FROM reservas r " +
                     "JOIN mesas m ON r.id_mesa = m.id_mesa " +
                     "WHERE r.fecha_reserva = ? AND r.hora_reserva = ? " +
                     "  AND r.estado_reserva = 'CONFIRMADA'";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(fecha));
            stmt.setTime(2, Time.valueOf(LocalTime.parse(hora)));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) mesas.add(rs.getString("numero_mesa"));
            }
        } catch (SQLException e) {
            log.error("Error al obtener mesas ocupadas", e);
        }
        return mesas;
    }

    /**
     * Comprueba conflicto de mesa/fecha/hora antes de guardar.
     * @param excludeIdReserva  ID a excluir (usar -1 al crear, id real al editar)
     */
    public boolean hayConflicto(int idMesa, LocalDate fecha, LocalTime hora, int excludeIdReserva) {
        String sql = "SELECT COUNT(*) FROM reservas " +
                     "WHERE id_mesa = ? AND fecha_reserva = ? AND hora_reserva = ? " +
                     "  AND estado_reserva = 'CONFIRMADA' AND id_reserva != ?";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idMesa);
            stmt.setDate(2, Date.valueOf(fecha));
            stmt.setTime(3, Time.valueOf(hora));
            stmt.setInt(4, excludeIdReserva);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            log.error("Error al comprobar conflicto de mesa", e);
        }
        return false;
    }

    /** Disponibilidad simple (sin excluir ninguna reserva). */
    public boolean estaDisponible(int idMesa, LocalDate fecha, String hora) {
        return !hayConflicto(idMesa, fecha, LocalTime.parse(hora), -1);
    }

    // ════════════════════════════════════════════════════════════
    // MODIFICAR / CANCELAR / NO-SHOW
    // ════════════════════════════════════════════════════════════

    /** Cambia el estado de la reserva. Valores: CONFIRMADA, CANCELADA, FINALIZADA, NO_PRESENTADO */
    public boolean cambiarEstado(int idReserva, String nuevoEstado) {
        String sql = "UPDATE reservas SET estado_reserva = ? WHERE id_reserva = ?";
        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, idReserva);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error al cambiar estado de reserva", e);
            return false;
        }
    }

    /**
     * Marca la reserva como NO_PRESENTADO.
     * Diferente de cancelar: aquí el cliente directamente no se presentó.
     * La preautorización queda retenida — esto es lo habitual en hostelería
     * para cubrir el coste de la mesa perdida.
     */
    public boolean marcarNoPresentado(int idReserva) {
        return cambiarEstado(idReserva, "No presentado");
    }

    /** Actualiza datos editables: mesa, fecha, hora, comensales, ocasion, peticiones, observaciones. */
    public boolean actualizarReserva(Reserva reserva) {
        String sql = "UPDATE reservas SET " +
                     "id_mesa = ?, fecha_reserva = ?, hora_reserva = ?, comensales = ?, " +
                     "ocasion = ?, peticiones_especiales = ?, observaciones = ? " +
                     "WHERE id_reserva = ?";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, reserva.getIdMesa());
            stmt.setDate(2, Date.valueOf(reserva.getFechaReserva()));
            stmt.setTime(3, Time.valueOf(reserva.getHoraReserva()));
            stmt.setInt(4, reserva.getComensales());
            stmt.setString(5, reserva.getOcasion());
            stmt.setString(6, reserva.getPeticionesEspeciales());
            stmt.setString(7, reserva.getObservaciones());
            stmt.setInt(8, reserva.getIdReserva());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("Error al actualizar reserva", e);
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    // RESOLUCIÓN DE IDs
    // ════════════════════════════════════════════════════════════

    /** Traduce "M1"-"M12" al id_mesa de la BD. Devuelve -1 si no existe. */
    public int resolverIdMesa(String numeroMesa) {
        String sql = "SELECT id_mesa FROM mesas WHERE numero_mesa = ?";
        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, numeroMesa);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt("id_mesa");
            }
        } catch (SQLException e) {
            log.error("Error al resolver id_mesa", e);
        }
        return -1;
    }

    // ════════════════════════════════════════════════════════════
    // UTILIDADES PRIVADAS
    // ════════════════════════════════════════════════════════════

    // Convierte una fila del ResultSet a un objeto Reserva
    // (hay que leer los campos por nombre de columna, no por índice, para que
    //  no se rompa si algún día cambia el orden del SELECT)
    private Reserva mapear(ResultSet rs) throws SQLException {
        Reserva r = new Reserva();
        r.setIdReserva(rs.getInt("id_reserva"));
        r.setIdCliente(rs.getInt("id_cliente"));
        r.setIdMesa(rs.getInt("id_mesa"));
        r.setFechaReserva(rs.getDate("fecha_reserva").toLocalDate());
        r.setHoraReserva(rs.getTime("hora_reserva").toLocalTime());
        r.setComensales(rs.getInt("comensales"));
        r.setEstadoReserva(rs.getString("estado_reserva"));
        r.setOcasion(rs.getString("ocasion"));
        r.setPeticionesEspeciales(rs.getString("peticiones_especiales"));
        r.setObservaciones(rs.getString("observaciones"));
        r.setNombreCliente(rs.getString("nombre_completo"));
        r.setNumeroMesa(rs.getString("numero_mesa"));
        return r;
    }
}
