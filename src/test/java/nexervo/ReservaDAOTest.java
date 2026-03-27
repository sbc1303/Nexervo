package nexervo;

import com.nexervo.datos.ReservaDAO;
import com.nexervo.modelo.Reserva;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración de ReservaDAO contra H2 en memoria.
 *
 * Estos tests comprueban que las consultas SQL son correctas y que
 * el mapeo ResultSet → Reserva funciona bien. No hay mocks: se ejecuta
 * SQL real contra una BD limpia creada antes de cada test.
 *
 * Añadido en v4 tras configurar H2 como BD de prueba.
 */
@DisplayName("ReservaDAO — integración con H2")
class ReservaDAOTest {

    private static ConexionH2Test conexion;
    private ReservaDAO dao;

    // ID de mesa M1 insertada en el schema de test (AUTO_INCREMENT → 1)
    private static final int ID_MESA_M1 = 1;

    @BeforeAll
    static void prepararBD() {
        conexion = new ConexionH2Test();
        ConexionH2Test.inicializar();
    }

    @BeforeEach
    void prepararDAO() {
        dao = new ReservaDAO(conexion);
        // Limpiar e insertar datos frescos antes de cada test.
        // IMPORTANTE: dos DELETE separados — un solo PreparedStatement con ";" no es
        // estándar JDBC y H2 puede ignorar silenciosamente la segunda sentencia.
        try (var conn = conexion.getConexion();
             var stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM reservas");
            stmt.execute("DELETE FROM clientes");
            stmt.execute(
                "INSERT INTO clientes (id_cliente, nombre_completo, telefono, email) " +
                "VALUES (1, 'Test Cliente', '600000001', 'test@test.com')"
            );
        } catch (Exception e) {
            throw new RuntimeException("Error preparando datos de test: " + e.getMessage(), e);
        }
    }

    // ── CREAR ────────────────────────────────────────────────────────

    @Test
    @DisplayName("crearReserva inserta correctamente y devuelve ID generado")
    void crearReserva_devuelveIdPositivo() {
        Reserva r = reservaDePrueba(LocalDate.of(2026, 6, 1), LocalTime.of(14, 0));
        int id = dao.crearReserva(r);
        assertTrue(id > 0, "El ID generado debe ser positivo");
    }

    @Test
    @DisplayName("crearReserva guarda todos los campos correctamente")
    void crearReserva_guardaCampos() {
        LocalDate fecha = LocalDate.of(2026, 6, 2);
        LocalTime hora  = LocalTime.of(21, 0);
        Reserva r = new Reserva(1, ID_MESA_M1, fecha, hora, 4,
                                "Cumpleaños", "Tarta sorpresa", "Sin gluten");
        int id = dao.crearReserva(r);

        List<Reserva> lista = dao.obtenerPorFecha(fecha);
        assertEquals(1, lista.size());
        Reserva guardada = lista.get(0);
        assertEquals(4,            guardada.getComensales());
        assertEquals("Cumpleaños", guardada.getOcasion());
        assertEquals("CONFIRMADA", guardada.getEstadoReserva());
    }

    // ── CONSULTAR ────────────────────────────────────────────────────

    @Test
    @DisplayName("obtenerPorFecha devuelve lista vacía si no hay reservas ese día")
    void obtenerPorFecha_sinReservas() {
        List<Reserva> lista = dao.obtenerPorFecha(LocalDate.of(2030, 1, 1));
        assertTrue(lista.isEmpty());
    }

    @Test
    @DisplayName("obtenerPorFecha devuelve las reservas de esa fecha ordenadas por hora")
    void obtenerPorFecha_ordenPorHora() {
        LocalDate fecha = LocalDate.of(2026, 7, 1);
        dao.crearReserva(reservaDePrueba(fecha, LocalTime.of(21, 30)));
        dao.crearReserva(reservaDePrueba(fecha, LocalTime.of(14, 0)));

        List<Reserva> lista = dao.obtenerPorFecha(fecha);
        assertEquals(2, lista.size());
        assertTrue(lista.get(0).getHoraReserva().isBefore(lista.get(1).getHoraReserva()),
                   "Las reservas deben estar ordenadas por hora ascendente");
    }

    @Test
    @DisplayName("obtenerPorCliente devuelve el historial completo del cliente")
    void obtenerPorCliente_devuelveHistorial() {
        LocalDate hoy = LocalDate.now();
        dao.crearReserva(reservaDePrueba(hoy, LocalTime.of(14, 0)));
        dao.crearReserva(reservaDePrueba(hoy.plusDays(1), LocalTime.of(21, 0)));

        List<Reserva> historial = dao.obtenerPorCliente(1);
        assertEquals(2, historial.size());
    }

    // ── CAMBIAR ESTADO ───────────────────────────────────────────────

    @Test
    @DisplayName("cambiarEstado actualiza correctamente el estado de la reserva")
    void cambiarEstado_actualizaCorrectamente() {
        int id = dao.crearReserva(reservaDePrueba(LocalDate.of(2026, 8, 1), LocalTime.of(14, 0)));
        boolean ok = dao.cambiarEstado(id, "FINALIZADA");

        assertTrue(ok);
        List<Reserva> lista = dao.obtenerPorFecha(LocalDate.of(2026, 8, 1));
        assertEquals("FINALIZADA", lista.get(0).getEstadoReserva());
    }

    @Test
    @DisplayName("marcarNoPresentado cambia el estado a NO_PRESENTADO")
    void marcarNoPresentado_cambiaEstado() {
        int id = dao.crearReserva(reservaDePrueba(LocalDate.of(2026, 9, 1), LocalTime.of(21, 0)));
        assertTrue(dao.marcarNoPresentado(id));
        List<Reserva> lista = dao.obtenerPorFecha(LocalDate.of(2026, 9, 1));
        assertEquals("No presentado", lista.get(0).getEstadoReserva());
    }

    // ── CONFLICTOS ───────────────────────────────────────────────────

    @Test
    @DisplayName("hayConflicto detecta mesa ya reservada a la misma hora")
    void hayConflicto_detectaChoque() {
        LocalDate fecha = LocalDate.of(2026, 10, 1);
        LocalTime hora  = LocalTime.of(14, 0);
        dao.crearReserva(reservaDePrueba(fecha, hora));

        assertTrue(dao.hayConflicto(ID_MESA_M1, fecha, hora, -1),
                   "Debe detectar conflicto para la misma mesa/fecha/hora");
    }

    @Test
    @DisplayName("hayConflicto devuelve false para mesa libre")
    void hayConflicto_mesaLibre() {
        assertFalse(dao.hayConflicto(ID_MESA_M1,
                                     LocalDate.of(2027, 1, 1),
                                     LocalTime.of(14, 0), -1));
    }

    // ── Utilidad ─────────────────────────────────────────────────────

    private Reserva reservaDePrueba(LocalDate fecha, LocalTime hora) {
        return new Reserva(1, ID_MESA_M1, fecha, hora, 2, "Ninguna", null, null);
    }
}
