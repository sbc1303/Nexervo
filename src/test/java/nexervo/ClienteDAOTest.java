package nexervo;

import com.nexervo.datos.ClienteDAO;
import com.nexervo.modelo.Cliente;
import com.nexervo.modelo.Intolerancia;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración de ClienteDAO contra H2 en memoria.
 *
 * Cubre el ciclo completo de un cliente: registro, búsqueda por teléfono,
 * vinculación de intolerancias y desvinculación.
 *
 * Añadido en v4 junto con ReservaDAOTest al configurar H2 para tests.
 */
@DisplayName("ClienteDAO — integración con H2")
class ClienteDAOTest {

    private static ConexionH2Test conexion;
    private ClienteDAO dao;

    @BeforeAll
    static void prepararBD() {
        conexion = new ConexionH2Test();
        ConexionH2Test.inicializar();
    }

    @BeforeEach
    void limpiarClientes() {
        dao = new ClienteDAO(conexion);
        try (var conn = conexion.getConexion();
             var stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM clientes_intolerancias");
            stmt.execute("DELETE FROM clientes");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ── REGISTRO ─────────────────────────────────────────────────────

    @Test
    @DisplayName("registrarCliente devuelve ID positivo al insertar un cliente nuevo")
    void registrarCliente_devuelveIdPositivo() {
        Cliente c = new Cliente("Ana García", "600111222", "ana@test.com", null);
        int id = dao.registrarCliente(c);
        assertTrue(id > 0, "El ID generado debe ser positivo");
    }

    @Test
    @DisplayName("registrarCliente guarda los datos correctamente en BD")
    void registrarCliente_guardaDatos() {
        Cliente c = new Cliente("Carlos Ruiz", "666777888", "carlos@test.com", "VIP");
        int id = dao.registrarCliente(c);

        Cliente recuperado = dao.buscarPorTelefono("666777888");
        assertNotNull(recuperado);
        assertEquals("Carlos Ruiz",    recuperado.getNombre());
        assertEquals("carlos@test.com", recuperado.getEmail());
    }

    // ── BÚSQUEDA ─────────────────────────────────────────────────────

    @Test
    @DisplayName("buscarPorTelefono devuelve null si el teléfono no existe")
    void buscarPorTelefono_noExiste() {
        assertNull(dao.buscarPorTelefono("000000000"));
    }

    @Test
    @DisplayName("buscarPorTelefono encuentra al cliente por teléfono exacto")
    void buscarPorTelefono_encuentraCliente() {
        Cliente c = new Cliente("María López", "611222333", null, null);
        dao.registrarCliente(c);

        Cliente encontrado = dao.buscarPorTelefono("611222333");
        assertNotNull(encontrado);
        assertEquals("María López", encontrado.getNombre());
    }

    // ── INTOLERANCIAS ────────────────────────────────────────────────

    @Test
    @DisplayName("vincularIntolerancia asocia la alergia al cliente correctamente")
    void vincularIntolerancia_asociaCorrectamente() {
        Cliente c = new Cliente("Pedro Sánchez", "622333444", null, null);
        int idCliente = dao.registrarCliente(c);

        // ID 1 = Gluten (insertado en schema_test.sql)
        dao.vincularIntolerancia(idCliente, 1);

        List<Intolerancia> intols = dao.obtenerIntoleranciasPorCliente(idCliente);
        assertEquals(1, intols.size());
        assertTrue(intols.get(0).getNombre().contains("Gluten"));
    }

    @Test
    @DisplayName("desvincularIntolerancia elimina la asociación correctamente")
    void desvincularIntolerancia_eliminaAsociacion() {
        Cliente c = new Cliente("Laura Martín", "633444555", null, null);
        int idCliente = dao.registrarCliente(c);

        dao.vincularIntolerancia(idCliente, 1);
        dao.vincularIntolerancia(idCliente, 2);
        dao.desvincularIntolerancia(idCliente, 1);

        List<Intolerancia> intols = dao.obtenerIntoleranciasPorCliente(idCliente);
        assertEquals(1, intols.size(), "Solo debe quedar una intolerancia tras desvincular");
        assertEquals(2, intols.get(0).getIdIntolerancia());
    }

    @Test
    @DisplayName("obtenerCatalogoIntolerancias devuelve todas las intolerancias del sistema")
    void obtenerCatalogo_devuelveTodas() {
        List<Intolerancia> catalogo = dao.obtenerCatalogoIntolerancias();
        // El schema_test.sql inserta 3 intolerancias
        assertEquals(3, catalogo.size());
    }
}
