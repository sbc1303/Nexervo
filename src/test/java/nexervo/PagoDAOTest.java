package nexervo;

import com.nexervo.datos.PagoDAO;
import com.nexervo.datos.ReservaDAO;
import com.nexervo.modelo.PreautorizacionPago;
import com.nexervo.modelo.Reserva;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de PagoDAO contra H2.
 *
 * Como preautorizaciones_pago tiene FK → reservas → clientes → mesas,
 * necesito insertar todo en orden antes de probar pagos.
 * El @BeforeEach lo hace desde cero para que cada test parta limpio.
 */
@DisplayName("PagoDAO — H2")
class PagoDAOTest {

    private static ConexionH2Test conexion;
    private PagoDAO    pagoDAO;
    private ReservaDAO reservaDAO;
    private int        idReservaPrueba;

    @BeforeAll
    static void prepararBD() {
        conexion = new ConexionH2Test();
        ConexionH2Test.inicializar();
    }

    @BeforeEach
    void prepararDatos() {
        pagoDAO    = new PagoDAO(conexion);
        reservaDAO = new ReservaDAO(conexion);

        try (var conn = conexion.getConexion();
             var stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM preautorizaciones_pago");
            stmt.execute("DELETE FROM reservas");
            stmt.execute("DELETE FROM clientes");
            stmt.execute(
                "INSERT INTO clientes (id_cliente, nombre_completo, telefono) " +
                "VALUES (1, 'Test Pago', '611000001')"
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Reserva r = new Reserva(1, 1, LocalDate.of(2026, 11, 1), LocalTime.of(14, 0),
                                2, "Ninguna", null, null);
        idReservaPrueba = reservaDAO.crearReserva(r);
        assertTrue(idReservaPrueba > 0);
    }

    @Test
    @DisplayName("crearPreautorizacion devuelve id positivo")
    void crear_idPositivo() {
        assertTrue(pagoDAO.crearPreautorizacion(idReservaPrueba, new BigDecimal("20.00")) > 0);
    }

    @Test
    @DisplayName("crearPreautorizacion guarda importe y estado PENDIENTE")
    void crear_estadoPendienteYImporte() {
        pagoDAO.crearPreautorizacion(idReservaPrueba, new BigDecimal("40.00"));

        PreautorizacionPago p = pagoDAO.obtenerPorReserva(idReservaPrueba);
        assertNotNull(p);
        assertEquals("PENDIENTE", p.getEstado());
        assertEquals(0, new BigDecimal("40.00").compareTo(p.getImporte()));
    }

    @Test
    @DisplayName("obtenerPorReserva devuelve null si no hay preautorización")
    void obtener_sinPreautorizacion_null() {
        assertNull(pagoDAO.obtenerPorReserva(idReservaPrueba));
    }

    @Test
    @DisplayName("obtenerPorReserva devuelve la preautorización existente")
    void obtener_devuelvePreautorizacion() {
        int idPago = pagoDAO.crearPreautorizacion(idReservaPrueba, new BigDecimal("30.00"));

        PreautorizacionPago p = pagoDAO.obtenerPorReserva(idReservaPrueba);
        assertNotNull(p);
        assertEquals(idPago, p.getIdPago());
        assertNotNull(p.getFechaCreacion());
    }

    @Test
    @DisplayName("cambiarEstado a AUTORIZADA funciona correctamente")
    void cambiarEstado_aAutorizada() {
        int idPago = pagoDAO.crearPreautorizacion(idReservaPrueba, new BigDecimal("20.00"));
        assertTrue(pagoDAO.cambiarEstado(idPago, "AUTORIZADA"));
        assertEquals("AUTORIZADA", pagoDAO.obtenerPorReserva(idReservaPrueba).getEstado());
    }

    @Test
    @DisplayName("cambiarEstado a DEVUELTA registra la fecha de gestión")
    void cambiarEstado_devuelta_registraFecha() {
        int idPago = pagoDAO.crearPreautorizacion(idReservaPrueba, new BigDecimal("20.00"));
        pagoDAO.cambiarEstado(idPago, "DEVUELTA");

        PreautorizacionPago p = pagoDAO.obtenerPorReserva(idReservaPrueba);
        assertEquals("DEVUELTA", p.getEstado());
        // fecha_gestion la pone NOW() en MySQL/H2 al actualizar el estado
        assertNotNull(p.getFechaGestion());
    }

    @Test
    @DisplayName("cambiarEstado con id que no existe devuelve false")
    void cambiarEstado_idInvalido_false() {
        assertFalse(pagoDAO.cambiarEstado(9999, "AUTORIZADA"));
    }
}
