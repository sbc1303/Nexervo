package com.nexervo.datos;

import com.nexervo.modelo.Cliente;
import com.nexervo.modelo.Intolerancia;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la tabla 'clientes' y operaciones relacionadas.
 *
 * Responsabilidades:
 *   - CRUD de clientes
 *   - Carga del catálogo de intolerancias (para el ComboBox del formulario)
 *   - Gestión de intolerancias vinculadas a un cliente concreto
 *
 * Todo lo relacionado con disponibilidad y reservas va en ReservaDAO.
 */
public class ClienteDAO {

    private static final Logger log = LoggerFactory.getLogger(ClienteDAO.class);
    private final ConexionBD conexion;

    public ClienteDAO() {
        this.conexion = new ConexionConMySQL();
    }

    /** Constructor para tests: permite inyectar cualquier ConexionBD (ej: H2). */
    public ClienteDAO(ConexionBD conexion) {
        this.conexion = conexion;
    }

    // ════════════════════════════════════════════════════════════
    // CRUD CLIENTES
    // ════════════════════════════════════════════════════════════

    /**
     * Registra un nuevo cliente en la base de datos.
     * Devuelve el id_cliente generado, o -1 si falla.
     * Usamos RETURN_GENERATED_KEYS para recuperar el ID
     * y poder vincularlo inmediatamente a una reserva.
     */
    public int registrarCliente(Cliente cliente) {
        String sql = "INSERT INTO clientes (nombre_completo, telefono, email, observaciones) " +
                     "VALUES (?, ?, ?, ?)";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getTelefono());
            stmt.setString(3, cliente.getEmail());
            stmt.setString(4, cliente.getObservaciones());
            stmt.executeUpdate();

            // Recuperar el ID autogenerado por MySQL
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }

        } catch (SQLException e) {
            log.error("Error al registrar cliente", e);
        }
        return -1;  // indica fallo
    }

    /**
     * Devuelve todos los clientes de la base de datos.
     * Usado para poblar la vista de gestión de clientes.
     */
    public List<Cliente> listarClientes() {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT id_cliente, nombre_completo, telefono, email, observaciones " +
                     "FROM clientes ORDER BY nombre_completo";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Cliente c = new Cliente();
                c.setIdCliente(rs.getInt("id_cliente"));
                c.setNombre(rs.getString("nombre_completo"));
                c.setTelefono(rs.getString("telefono"));
                c.setEmail(rs.getString("email"));
                c.setObservaciones(rs.getString("observaciones"));
                lista.add(c);
            }

        } catch (SQLException e) {
            log.error("Error al listar clientes", e);
        }
        return lista;
    }

    /**
     * Busca un cliente por número de teléfono.
     * Útil para detectar si el cliente ya existe al crear una reserva.
     * Devuelve null si no se encuentra.
     */
    public Cliente buscarPorTelefono(String telefono) {
        String sql = "SELECT id_cliente, nombre_completo, telefono, email, observaciones " +
                     "FROM clientes WHERE telefono = ?";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, telefono);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Cliente c = new Cliente();
                    c.setIdCliente(rs.getInt("id_cliente"));
                    c.setNombre(rs.getString("nombre_completo"));
                    c.setTelefono(rs.getString("telefono"));
                    c.setEmail(rs.getString("email"));
                    c.setObservaciones(rs.getString("observaciones"));
                    return c;
                }
            }

        } catch (SQLException e) {
            log.error("Error al buscar cliente por teléfono", e);
        }
        return null;
    }

    /**
     * Busca un cliente por teléfono Y email (doble verificación para mayor seguridad).
     * Devuelve null si no hay coincidencia con ambos datos.
     */
    public Cliente buscarPorTelefonoYEmail(String telefono, String email) {
        String sql = "SELECT id_cliente, nombre_completo, telefono, email, observaciones " +
                     "FROM clientes WHERE telefono = ? AND LOWER(email) = LOWER(?)";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, telefono);
            stmt.setString(2, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Cliente c = new Cliente();
                    c.setIdCliente(rs.getInt("id_cliente"));
                    c.setNombre(rs.getString("nombre_completo"));
                    c.setTelefono(rs.getString("telefono"));
                    c.setEmail(rs.getString("email"));
                    c.setObservaciones(rs.getString("observaciones"));
                    return c;
                }
            }

        } catch (SQLException e) {
            log.error("Error al buscar cliente por teléfono y email", e);
        }
        return null;
    }

    /**
     * Actualiza los datos de un cliente existente.
     * Devuelve true si la actualización fue exitosa.
     */
    public boolean actualizarCliente(Cliente cliente) {
        String sql = "UPDATE clientes SET nombre_completo = ?, telefono = ?, " +
                     "email = ?, observaciones = ? WHERE id_cliente = ?";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cliente.getNombre());
            stmt.setString(2, cliente.getTelefono());
            stmt.setString(3, cliente.getEmail());
            stmt.setString(4, cliente.getObservaciones());
            stmt.setInt(5, cliente.getIdCliente());
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            log.error("Error al actualizar cliente", e);
            return false;
        }
    }

    /**
     * Elimina un cliente por ID.
     * Al estar configurado ON DELETE CASCADE en la FK de reservas,
     * eliminar el cliente elimina también sus reservas.
     * Solo debe ejecutarlo el rol ADMIN.
     */
    public boolean eliminarCliente(int idCliente) {
        String sql = "DELETE FROM clientes WHERE id_cliente = ?";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idCliente);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            log.error("Error al eliminar cliente", e);
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    // INTOLERANCIAS
    // ════════════════════════════════════════════════════════════

    /**
     * Devuelve el catálogo completo de intolerancias ordenado por tipo y nombre.
     * El controlador usa este listado para construir el ComboBox agrupado:
     *   - tipo ALERGENO_UE  → primeros 14 (sección "14 ALÉRGENOS OFICIALES UE")
     *   - tipo INTOLERANCIA → resto      (sección "INTOLERANCIAS COMUNES")
     */
    public List<Intolerancia> obtenerCatalogoIntolerancias() {
        List<Intolerancia> lista = new ArrayList<>();
        // Ordenamos: primero los ALERGENO_UE (por id, que coincide con el orden legal),
        // luego las INTOLERANCIA comunes.
        String sql = "SELECT id_intolerancia, nombre, tipo FROM intolerancias " +
                     "ORDER BY FIELD(tipo, 'ALERGENO_UE', 'INTOLERANCIA'), id_intolerancia";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(new Intolerancia(
                        rs.getInt("id_intolerancia"),
                        rs.getString("nombre"),
                        rs.getString("tipo")
                ));
            }

        } catch (SQLException e) {
            log.error("Error al obtener catálogo de intolerancias", e);
        }
        return lista;
    }

    /**
     * Devuelve las intolerancias registradas para un cliente concreto.
     * Se usa para mostrar la alerta visual al consultar una reserva (RF-14).
     */
    public List<Intolerancia> obtenerIntoleranciasPorCliente(int idCliente) {
        List<Intolerancia> lista = new ArrayList<>();
        String sql = "SELECT i.id_intolerancia, i.nombre, i.tipo " +
                     "FROM intolerancias i " +
                     "JOIN clientes_intolerancias ci ON i.id_intolerancia = ci.id_intolerancia " +
                     "WHERE ci.id_cliente = ?";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idCliente);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new Intolerancia(
                            rs.getInt("id_intolerancia"),
                            rs.getString("nombre"),
                            rs.getString("tipo")
                    ));
                }
            }

        } catch (SQLException e) {
            log.error("Error al obtener intolerancias del cliente", e);
        }
        return lista;
    }

    /**
     * Vincula una intolerancia a un cliente.
     * La tabla clientes_intolerancias tiene PK compuesta,
     * así que insertar duplicados lanza excepción (se ignora silenciosamente).
     */
    public boolean vincularIntolerancia(int idCliente, int idIntolerancia) {
        String sql = "INSERT IGNORE INTO clientes_intolerancias (id_cliente, id_intolerancia) " +
                     "VALUES (?, ?)";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idCliente);
            stmt.setInt(2, idIntolerancia);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            log.error("Error al vincular intolerancia", e);
            return false;
        }
    }

    /**
     * Desvincula una intolerancia de un cliente.
     */
    public boolean desvincularIntolerancia(int idCliente, int idIntolerancia) {
        String sql = "DELETE FROM clientes_intolerancias " +
                     "WHERE id_cliente = ? AND id_intolerancia = ?";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idCliente);
            stmt.setInt(2, idIntolerancia);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            log.error("Error al desvincular intolerancia", e);
            return false;
        }
    }
}
