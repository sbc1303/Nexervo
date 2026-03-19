package datos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Clase encargada de gestionar la conexión a la base de datos MySQL.
 */
public class ConexionConMySQL {

    // URL de conexión JDBC
    private static final String URL =
            "jdbc:mysql://localhost:3306/nexervo_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Europe/Madrid";

    // Usuario de la base de datos
    private static final String USUARIO = "root";

    // Contraseña (IMPORTANTE: ajusta según tu configuración)
    private static final String CLAVE = ""; // ← déjalo así si no tienes contraseña

    /**
     * Devuelve una conexión activa a la base de datos.
     * @return Connection o null si falla
     */
    public static Connection getConexion() {
        try {
            return DriverManager.getConnection(URL, USUARIO, CLAVE);
        } catch (SQLException e) {
            System.err.println("Error al conectar con nexervo_db: " + e.getMessage());
            return null;
        }
    }
}