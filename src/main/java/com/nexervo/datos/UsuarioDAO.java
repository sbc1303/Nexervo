package com.nexervo.datos;

import com.nexervo.modelo.Usuario;

import java.sql.*;

/**
 * DAO para la tabla 'usuarios'.
 *
 * Responsabilidades:
 *   - Autenticar credenciales de login
 *   - Gestión básica de usuarios (solo accesible por ADMIN)
 */
public class UsuarioDAO {

    private final ConexionConMySQL conexion;

    public UsuarioDAO() {
        this.conexion = new ConexionConMySQL();
    }

    /**
     * Autentica un usuario contra la base de datos.
     *
     * Devuelve el objeto Usuario con su rol si las credenciales
     * son correctas y el usuario está activo.
     * Devuelve null si el login falla (credenciales incorrectas,
     * usuario inactivo o error de conexión).
     *
     * NOTA: en esta fase la contraseña se compara en texto plano.
     * En producción se usaría BCrypt: BCrypt.checkpw(input, hashBD).
     */
    public Usuario autenticar(String usuario, String contrasena) {
        String sql = "SELECT id_usuario, nombre, usuario, rol, activo " +
                     "FROM usuarios " +
                     "WHERE usuario = ? AND contrasena = ? AND activo = 1";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario);
            stmt.setString(2, contrasena);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario();
                    u.setIdUsuario(rs.getInt("id_usuario"));
                    u.setNombre(rs.getString("nombre"));
                    u.setUsuario(rs.getString("usuario"));
                    u.setRol(rs.getString("rol"));
                    u.setActivo(rs.getBoolean("activo"));
                    // La contraseña nunca se carga en memoria desde la BD
                    return u;
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al autenticar usuario: " + e.getMessage());
        }
        return null;  // login fallido
    }

    /**
     * Comprueba si un nombre de usuario ya existe en la BD.
     * Útil para validar antes de crear un usuario nuevo.
     */
    public boolean existeUsuario(String usuario) {
        String sql = "SELECT COUNT(*) FROM usuarios WHERE usuario = ?";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            System.err.println("Error al comprobar usuario: " + e.getMessage());
        }
        return false;
    }

    /**
     * Crea un nuevo usuario en el sistema.
     * Solo debe llamarse desde el panel de administración (rol ADMIN).
     * Devuelve true si la inserción fue exitosa.
     */
    public boolean crearUsuario(Usuario usuario) {
        String sql = "INSERT INTO usuarios (nombre, usuario, contrasena, rol) " +
                     "VALUES (?, ?, ?, ?)";

        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, usuario.getNombre());
            stmt.setString(2, usuario.getUsuario());
            stmt.setString(3, usuario.getContrasena());
            stmt.setString(4, usuario.getRol());
            return stmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al crear usuario: " + e.getMessage());
            return false;
        }
    }

    /**
     * Activa o desactiva un usuario sin eliminarlo.
     * Preferible a borrar: mantiene el historial de reservas intacto.
     */
    public boolean cambiarEstadoUsuario(int idUsuario, boolean activo) {
        String sql = "UPDATE usuarios SET activo = ? WHERE id_usuario = ?";
        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, activo);
            stmt.setInt(2, idUsuario);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al cambiar estado de usuario: " + e.getMessage());
            return false;
        }
    }

    /** Devuelve todos los usuarios con rol EMPLEADO (para la tabla de gestión). */
    public java.util.List<Usuario> listarEmpleados() {
        java.util.List<Usuario> lista = new java.util.ArrayList<>();
        String sql = "SELECT id_usuario, nombre, usuario, rol, activo FROM usuarios ORDER BY rol, nombre";
        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Usuario u = new Usuario();
                u.setIdUsuario(rs.getInt("id_usuario"));
                u.setNombre(rs.getString("nombre"));
                u.setUsuario(rs.getString("usuario"));
                u.setRol(rs.getString("rol"));
                u.setActivo(rs.getBoolean("activo"));
                lista.add(u);
            }
        } catch (SQLException e) {
            System.err.println("Error al listar empleados: " + e.getMessage());
        }
        return lista;
    }

    /** Cambia la contraseña de un usuario. */
    public boolean cambiarContrasena(int idUsuario, String nuevaContrasena) {
        String sql = "UPDATE usuarios SET contrasena = ? WHERE id_usuario = ?";
        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nuevaContrasena);
            stmt.setInt(2, idUsuario);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al cambiar contraseña: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza nombre, login y rol de un usuario existente.
     * La contraseña se cambia por separado con cambiarContrasena().
     */
    public boolean actualizarUsuario(int idUsuario, String nombre, String usuario, String rol) {
        String sql = "UPDATE usuarios SET nombre = ?, usuario = ?, rol = ? WHERE id_usuario = ?";
        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nombre);
            stmt.setString(2, usuario);
            stmt.setString(3, rol);
            stmt.setInt(4, idUsuario);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar usuario: " + e.getMessage());
            return false;
        }
    }

    /** Elimina un usuario permanentemente. Solo usar para EMPLEADO, nunca para ADMIN. */
    public boolean eliminarUsuario(int idUsuario) {
        String sql = "DELETE FROM usuarios WHERE id_usuario = ? AND rol != 'ADMIN'";
        try (Connection conn = conexion.getConexion();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar usuario: " + e.getMessage());
            return false;
        }
    }
}
