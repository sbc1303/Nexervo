package nexervo;

import com.nexervo.datos.ConexionBD;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Implementación de ConexionBD para tests, usando H2 en memoria.
 *
 * Usa MODE=MySQL para que H2 entienda la sintaxis MySQL que usan los DAOs.
 * DB_CLOSE_DELAY=-1 mantiene la BD abierta mientras la JVM esté en marcha,
 * lo que permite que varios tests del mismo ciclo compartan el mismo esquema.
 *
 * Cada clase de test llama a inicializar() antes del primer test para
 * recrear las tablas desde schema_test.sql, garantizando un estado limpio.
 */
public class ConexionH2Test implements ConexionBD {

    private static final String URL =
        "jdbc:h2:mem:nexervo_test;MODE=MySQL;DB_CLOSE_DELAY=-1;NON_KEYWORDS=VALUE";

    /** Crea las tablas y datos base a partir de schema_test.sql. */
    public static void inicializar() {
        try (Connection conn = DriverManager.getConnection(URL, "sa", "");
             Statement stmt = conn.createStatement()) {

            // Eliminar tablas en orden inverso de dependencia para empezar limpio
            stmt.execute("DROP TABLE IF EXISTS preautorizaciones_pago");
            stmt.execute("DROP TABLE IF EXISTS clientes_intolerancias");
            stmt.execute("DROP TABLE IF EXISTS reservas");
            stmt.execute("DROP TABLE IF EXISTS intolerancias");
            stmt.execute("DROP TABLE IF EXISTS mesas");
            stmt.execute("DROP TABLE IF EXISTS clientes");
            stmt.execute("DROP TABLE IF EXISTS usuarios");

            // Ejecutar schema desde el fichero de recursos de test.
            // Al dividir por ";" puede ocurrir que un fragmento empiece por un
            // comentario (--) seguido de una sentencia SQL real en la misma línea
            // siguiente. El filtro anterior descartaba el bloque entero si el
            // primer carácter era "--", saltándose los INSERT de datos de prueba.
            // La solución correcta es eliminar las LÍNEAS de comentario antes de
            // evaluar si el fragmento tiene contenido ejecutable.
            String sql = leerRecurso("/schema_test.sql");
            for (String sentencia : sql.split(";")) {
                // Quitar líneas que sean solo comentarios
                String s = Arrays.stream(sentencia.split("\n"))
                        .filter(linea -> !linea.trim().startsWith("--"))
                        .collect(Collectors.joining("\n"))
                        .trim();
                if (!s.isEmpty()) {
                    stmt.execute(s);
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al inicializar BD de test H2: " + e.getMessage(), e);
        }
    }

    @Override
    public Connection getConexion() {
        try {
            return DriverManager.getConnection(URL, "sa", "");
        } catch (SQLException e) {
            throw new RuntimeException("Error obteniendo conexión H2: " + e.getMessage(), e);
        }
    }

    private static String leerRecurso(String path) throws Exception {
        InputStream is = ConexionH2Test.class.getResourceAsStream(path);
        if (is == null) throw new IllegalArgumentException("Recurso no encontrado: " + path);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }
}
