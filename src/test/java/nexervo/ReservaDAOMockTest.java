package nexervo;

import com.nexervo.datos.ConexionBD;
import com.nexervo.datos.ReservaDAO;
import com.nexervo.modelo.Reserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests con Mockito para ReservaDAO.
 *
 * Aquí no hay BD real: simulo que la conexión falla para comprobar
 * que el DAO devuelve valores seguros en lugar de propagar la excepción
 * hasta la UI (lo que haría que la app petara con un stacktrace).
 *
 * La idea surgió después de que en desarrollo me saltara una excepción
 * no controlada al probar sin MySQL levantado — quería asegurarme de que
 * eso no le pasa al usuario final.
 */
@DisplayName("ReservaDAO — mocks Mockito")
class ReservaDAOMockTest {

    private ConexionBD  mockConexion;
    private Connection  mockConn;
    private ReservaDAO  dao;

    @BeforeEach
    void setUp() throws SQLException {
        mockConexion = mock(ConexionBD.class);
        mockConn     = mock(Connection.class);
        when(mockConexion.getConexion()).thenReturn(mockConn);
        dao = new ReservaDAO(mockConexion);
    }

    @Test
    @DisplayName("crearReserva devuelve -1 si la BD falla")
    void crearReserva_fallaBD_menosUno() throws SQLException {
        when(mockConn.prepareStatement(anyString(), anyInt()))
            .thenThrow(new SQLException("BD caída"));

        Reserva r = new Reserva(1, 1, LocalDate.of(2026, 6, 1),
                                LocalTime.of(14, 0), 2, "Ninguna", null, null);

        assertEquals(-1, dao.crearReserva(r));
    }

    @Test
    @DisplayName("obtenerPorFecha devuelve lista vacía si la BD falla")
    void obtenerPorFecha_fallaBD_listaVacia() throws SQLException {
        when(mockConn.prepareStatement(anyString()))
            .thenThrow(new SQLException("Timeout"));

        List<Reserva> res = dao.obtenerPorFecha(LocalDate.now());
        assertNotNull(res);
        assertTrue(res.isEmpty());
    }

    @Test
    @DisplayName("cambiarEstado devuelve false si la BD falla")
    void cambiarEstado_fallaBD_false() throws SQLException {
        when(mockConn.prepareStatement(anyString()))
            .thenThrow(new SQLException("Error de red"));

        assertFalse(dao.cambiarEstado(1, "FINALIZADA"));
    }

    @Test
    @DisplayName("hayConflicto devuelve false (seguro) si la BD falla")
    void hayConflicto_fallaBD_false() throws SQLException {
        when(mockConn.prepareStatement(anyString()))
            .thenThrow(new SQLException("BD no disponible"));

        // Preferimos no bloquear la reserva por un error técnico
        assertFalse(dao.hayConflicto(1, LocalDate.of(2026, 6, 1), LocalTime.of(14, 0), -1));
    }

    @Test
    @DisplayName("obtenerPorCliente devuelve lista vacía si la BD falla")
    void obtenerPorCliente_fallaBD_listaVacia() throws SQLException {
        when(mockConn.prepareStatement(anyString()))
            .thenThrow(new SQLException("Fallo de red"));

        List<Reserva> res = dao.obtenerPorCliente(1);
        assertNotNull(res);
        assertTrue(res.isEmpty());
    }

    @Test
    @DisplayName("getConexion se llama exactamente una vez por consulta")
    void obtenerPorFecha_unaLlamadaAGetConexion() throws SQLException {
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);
        when(mockStmt.executeQuery()).thenThrow(new SQLException("executeQuery fallido"));

        dao.obtenerPorFecha(LocalDate.now());

        verify(mockConexion, times(1)).getConexion();
    }
}
