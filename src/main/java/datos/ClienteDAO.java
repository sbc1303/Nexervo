package datos;

import modelo.Cliente;
import modelo.Intolerancia;

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

    private final ConexionConMySQL conexion;

    public ClienteDAO() {
        this.conexion = new ConexionConMySQL();
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

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al registrar cliente: " + e.getMessage());
        }

        return -1;
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
            System.err.println("Error al listar clientes: " + e.getMessage());
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
            System.err.println("Error al buscar cliente: " + e.getMessage());
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
            System.err.println("Error al actualizar cliente: " + e.getMessage());
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
            System.err.println("Error al eliminar cliente: " + e.getMessage());
            return false;
        }
    }

    // ════════════════════════════════════════════════════════════
    // INTOLERANCIAS
    // ════════════════════════════════════════════════════════════

    /**
     * Devuelve el catálogo completo de intolerancias ordenado por tipo y nombre.
     * El controlador usa este listado para construir el ComboBox agrupado:
     *   - tipo ALERGENO_UE  → primeros 14
     *   - tipo INTOLERANCIA → resto
     */
    public List<Intolerancia> obtenerCatalogoIntolerancias() {
        List<Intolerancia> lista = new ArrayList<>();
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
            System.err.println("Error al obtener catálogo de intolerancias: " + e.getMessage());
        }

        return lista;
    }

    /**
     * Devuelve las intolerancias registradas para un cliente concreto.
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
            System.err.println("Error al obtener intolerancias del cliente: " + e.getMessage());
        }

        return lista;
    }

    /**
     * Vincula una intolerancia a un cliente.
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
            System.err.println("Error al vincular intolerancia: " + e.getMessage());
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
            System.err.println("Error al desvincular intolerancia: " + e.getMessage());
            return false;
        }
    }

    /**
     * Devuelve solo los nombres de todas las intolerancias del catálogo.
     */
    public List<String> obtenerIntolerancias() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT nombre FROM intolerancias " +
                "ORDER BY FIELD(tipo, 'ALERGENO_UE', 'INTOLERANCIA'), id_intolerancia";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(rs.getString("nombre"));
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener intolerancias: " + e.getMessage());
        }

        return lista;
    }

    /**
     * Devuelve los nombres de las intolerancias de un cliente.
     */
    public List<String> obtenerIntoleranciasDeCliente(int idCliente) {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT i.nombre " +
                "FROM intolerancias i " +
                "JOIN clientes_intolerancias ci ON i.id_intolerancia = ci.id_intolerancia " +
                "WHERE ci.id_cliente = ? " +
                "ORDER BY i.nombre";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idCliente);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(rs.getString("nombre"));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener intolerancias del cliente: " + e.getMessage());
        }

        return lista;
    }

    /**
     * Obtiene un cliente por su ID.
     */
    public Cliente obtenerClientePorId(int idCliente) {
        String sql = "SELECT id_cliente, nombre_completo, telefono, email, observaciones " +
                "FROM clientes WHERE id_cliente = ?";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, idCliente);

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
            System.err.println("Error al obtener cliente por ID: " + e.getMessage());
        }

        return null;
    }

    /**
     * Busca clientes por nombre, teléfono o email.
     */
    public List<Cliente> buscarClientes(String texto) {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT id_cliente, nombre_completo, telefono, email, observaciones " +
                "FROM clientes " +
                "WHERE nombre_completo LIKE ? OR telefono LIKE ? OR email LIKE ? " +
                "ORDER BY nombre_completo";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String filtro = "%" + texto + "%";
            stmt.setString(1, filtro);
            stmt.setString(2, filtro);
            stmt.setString(3, filtro);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Cliente c = new Cliente();
                    c.setIdCliente(rs.getInt("id_cliente"));
                    c.setNombre(rs.getString("nombre_completo"));
                    c.setTelefono(rs.getString("telefono"));
                    c.setEmail(rs.getString("email"));
                    c.setObservaciones(rs.getString("observaciones"));
                    lista.add(c);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar clientes: " + e.getMessage());
        }

        return lista;
    }
}