package com.nexervo.datos;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Conexión JDBC centralizada con nexervo_db.
 *
 * Orden de búsqueda de configuración:
 *   1. config.properties en el directorio de trabajo (junto al JAR)
 *   2. config.properties en el classpath (resources/config.properties)
 *   3. Valores por defecto hardcodeados (localhost/root/sin clave)
 *
 * Todos los DAO la usan dentro de try-with-resources:
 * la conexión se cierra automáticamente al salir del bloque.
 */
public class ConexionConMySQL implements ConexionBD {

    private static final String URL;
    private static final String USUARIO;
    private static final String CLAVE;

    static {
        Properties props = new Properties();

        // 1) Intentar leer del directorio de trabajo (configurable sin recompilar)
        boolean cargado = false;
        try (InputStream is = new FileInputStream("config.properties")) {
            props.load(is);
            cargado = true;
        } catch (IOException ignored) {}

        // 2) Intentar leer del classpath (dentro del JAR)
        if (!cargado) {
            try (InputStream is = ConexionConMySQL.class.getResourceAsStream("/config.properties")) {
                if (is != null) props.load(is);
            } catch (IOException ignored) {}
        }

        // 3) Construir URL a partir de los valores cargados (o valores por defecto)
        String host   = props.getProperty("db.host",    "localhost");
        String port   = props.getProperty("db.port",    "3306");
        String nombre = props.getProperty("db.nombre",  "nexervo_db");
        URL     = "jdbc:mysql://" + host + ":" + port + "/" + nombre
                + "?useSSL=false&serverTimezone=Europe/Madrid&allowPublicKeyRetrieval=true";
        USUARIO = props.getProperty("db.usuario", "root");
        CLAVE   = props.getProperty("db.clave",   "");
    }

    /**
     * Abre y devuelve una conexión a MySQL.
     * Nunca devuelve null: lanza RuntimeException si no puede conectar,
     * de forma que los DAO no necesitan comprobar null explícitamente.
     *
     * @throws RuntimeException si el driver no está disponible o la BD no responde
     */
    public Connection getConexion() {
        try {
            Connection conn = DriverManager.getConnection(URL, USUARIO, CLAVE);
            if (conn == null || conn.isClosed()) {
                throw new RuntimeException("La conexión a la base de datos devolvió null.");
            }
            return conn;
        } catch (SQLException e) {
            throw new RuntimeException(
                "[NEXERVO] No se pudo conectar a la base de datos.\n" +
                "Comprueba que MySQL está arrancado y que config.properties es correcto.\n" +
                "Detalle: " + e.getMessage(), e);
        }
    }
}
