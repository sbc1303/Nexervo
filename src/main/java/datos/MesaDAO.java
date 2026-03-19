package datos;

import modelo.Mesa;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO para la tabla 'mesas'.
 *
 * Responsabilidades:
 *   - Cargar el inventario de mesas desde la BD
 *   - Actualizar el estado de una mesa
 *
 * En fases futuras este DAO alimentará un mapa de sala
 * generado dinámicamente en lugar del GridPane estático del FXML.
 */
public class MesaDAO {

    private final ConexionConMySQL conexion;

    public MesaDAO() {
        this.conexion = new ConexionConMySQL();
    }

    /**
     * Devuelve todas las mesas ordenadas por número.
     * Se usa para poblar el mapa de sala y la vista de administración.
     */
    public List<Mesa> listarMesas() {
        List<Mesa> lista = new ArrayList<>();
        String sql = "SELECT id_mesa, numero_mesa, capacidad, estado, descripcion " +
                     "FROM mesas ORDER BY id_mesa";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                lista.add(new Mesa(
                        rs.getInt("id_mesa"),
                        rs.getString("numero_mesa"),
                        rs.getInt("capacidad"),
                        rs.getString("descripcion"),
                        rs.getString("estado")
                ));
            }

        } catch (SQLException e) {
            System.err.println("Error al listar mesas: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Busca una mesa por su número visual (ej: "M1").
     * Devuelve null si no existe.
     */
    public Mesa buscarPorNumero(String numeroMesa) {
        String sql = "SELECT id_mesa, numero_mesa, capacidad, estado, descripcion " +
                     "FROM mesas WHERE numero_mesa = ?";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, numeroMesa);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Mesa(
                            rs.getInt("id_mesa"),
                            rs.getString("numero_mesa"),
                            rs.getInt("capacidad"),
                            rs.getString("descripcion"),
                            rs.getString("estado")
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar mesa: " + e.getMessage());
        }
        return null;
    }

    /**
     * Actualiza el estado permanente de una mesa.
     * Valores válidos: "LIBRE", "OCUPADA", "RESERVADA"
     *
     * Nota: el estado dinámico por turno se gestiona en ReservaDAO,
     * no aquí. Este método es para cambios manuales del administrador
     * (ej: cerrar una mesa por mantenimiento).
     */
    public boolean actualizarEstado(int idMesa, String nuevoEstado) {
        String sql = "UPDATE mesas SET estado = ? WHERE id_mesa = ?";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nuevoEstado);
            stmt.setInt(2, idMesa);
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al actualizar estado de mesa: " + e.getMessage());
            return false;
        }
    }
}
