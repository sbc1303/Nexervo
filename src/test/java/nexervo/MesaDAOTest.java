package nexervo;

import com.nexervo.datos.MesaDAO;
import com.nexervo.modelo.Mesa;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de MesaDAO contra H2.
 *
 * El schema de test ya inserta M1, M2 y M3 así que no hace falta
 * insertar nada — solo verifico que los métodos leen y modifican bien.
 * Antes de cada test restauro el estado a LIBRE para que no se contaminen.
 */
@DisplayName("MesaDAO — H2")
class MesaDAOTest {

    private static ConexionH2Test conexion;
    private MesaDAO dao;

    @BeforeAll
    static void prepararBD() {
        conexion = new ConexionH2Test();
        ConexionH2Test.inicializar();
    }

    @BeforeEach
    void resetEstados() {
        dao = new MesaDAO(conexion);
        try (var conn = conexion.getConexion();
             var stmt = conn.createStatement()) {
            stmt.execute("UPDATE mesas SET estado = 'LIBRE'");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("listarMesas devuelve las 3 mesas del schema")
    void listarMesas_tresResultados() {
        assertEquals(3, dao.listarMesas().size());
    }

    @Test
    @DisplayName("listarMesas — capacidades correctas para M1 y M3")
    void listarMesas_capacidades() {
        List<Mesa> mesas = dao.listarMesas();

        Mesa m1 = mesas.stream().filter(m -> "M1".equals(m.getNumeroMesa())).findFirst().orElse(null);
        Mesa m3 = mesas.stream().filter(m -> "M3".equals(m.getNumeroMesa())).findFirst().orElse(null);

        assertNotNull(m1);
        assertEquals(2, m1.getCapacidad());
        assertNotNull(m3);
        assertEquals(6, m3.getCapacidad());
    }

    @Test
    @DisplayName("todas las mesas empiezan en estado LIBRE")
    void listarMesas_estadoLibreInicial() {
        long libres = dao.listarMesas().stream()
                        .filter(m -> "LIBRE".equals(m.getEstado())).count();
        assertEquals(3, libres);
    }

    @Test
    @DisplayName("buscarPorNumero encuentra M2 con datos correctos")
    void buscarPorNumero_encontrada() {
        Mesa m = dao.buscarPorNumero("M2");
        assertNotNull(m);
        assertEquals(4, m.getCapacidad());
        assertEquals("LIBRE", m.getEstado());
    }

    @Test
    @DisplayName("buscarPorNumero devuelve null para una mesa que no existe")
    void buscarPorNumero_noExiste() {
        assertNull(dao.buscarPorNumero("M99"));
    }

    @Test
    @DisplayName("actualizarEstado cambia M1 a OCUPADA")
    void actualizarEstado_aOcupada() {
        boolean ok = dao.actualizarEstado(1, "OCUPADA"); // id=1 → M1
        assertTrue(ok);
        assertEquals("OCUPADA", dao.buscarPorNumero("M1").getEstado());
    }

    @Test
    @DisplayName("actualizarEstado con id inexistente devuelve false")
    void actualizarEstado_idInvalido() {
        assertFalse(dao.actualizarEstado(999, "RESERVADA"));
    }
}
