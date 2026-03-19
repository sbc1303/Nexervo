package datos;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Gestiona la conexión JDBC con la base de datos nexervo_db.
 *
 * Con JDBC 4.0+ (incluido en mysql-connector-j 8.x), el driver se
 * registra automáticamente mediante ServiceLoader. El Class.forName()
 * explícito ya no es necesario y se ha eliminado.
 *
 * USO: cada DAO llama a getConexion() dentro de un try-with-resources
 * para garantizar el cierre automático de la conexión.
 */
public class ConexionConMySQL {

    private static final String URL     = "jdbc:mysql://localhost:3306/nexervo_db" +
                                          "?useSSL=false&serverTimezone=Europe/Madrid";
    private static final String USUARIO = "root";
    // TODO: cambiar antes de desplegar en producción
    private static final String CLAVE   = "MySQLBermejo1303!";

    /**
     * Devuelve una conexión activa a nexervo_db.
     * Devuelve null si la conexión falla (el error se imprime en consola).
     */
    public Connection getConexion() {
        try {
            return DriverManager.getConnection(URL, USUARIO, CLAVE);
        } catch (SQLException e) {
            System.err.println("Error al conectar con nexervo_db: " + e.getMessage());
            return null;
        }
    }
}
