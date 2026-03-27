package com.nexervo.datos;

import java.sql.Connection;

/**
 * Abstracción de la fuente de conexión a base de datos.
 *
 * Introduce esta interfaz para poder inyectar una conexión H2 en los tests
 * sin tocar el código de producción. En producción los DAO usan ConexionConMySQL;
 * en los tests de integración usan ConexionH2Test.
 *
 * Patrón: Strategy / Dependency Injection mínimo, sin framework.
 * Añadido en v4 para hacer los DAOs testeables con base de datos en memoria.
 */
public interface ConexionBD {

    /**
     * Devuelve una conexión activa a la base de datos.
     * El llamante es responsable de cerrarla (preferiblemente con try-with-resources).
     *
     * @throws RuntimeException si no puede establecer la conexión
     */
    Connection getConexion();
}
