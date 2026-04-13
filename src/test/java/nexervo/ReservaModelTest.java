package nexervo;

import com.nexervo.modelo.Reserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios del modelo Reserva.
 * Solo compruebo la lógica pura del modelo — sin conexión a BD ni JavaFX.
 *
 * Me centré en los métodos nuevos que añadí en v3 (tieneOcasionEspecial, isEditable)
 * porque la lógica de getters/setters básicos no tiene mucho que testear.
 * Los tests de DAO los dejaría para cuando tenga BD de prueba configurada.
 */
@DisplayName("Modelo Reserva")
class ReservaModelTest {

    @Test
    @DisplayName("Reserva creada con constructor completo contiene los datos correctos")
    void constructorCompleto() {
        LocalDate fecha = LocalDate.of(2026, 6, 15);
        LocalTime hora  = LocalTime.of(14, 0);

        Reserva r = new Reserva(1, 3, fecha, hora, 2, "Cumpleaños", "Tarta sorpresa", "Sin gluten");

        assertEquals(1,              r.getIdCliente());
        assertEquals(3,              r.getIdMesa());
        assertEquals(fecha,          r.getFechaReserva());
        assertEquals(hora,           r.getHoraReserva());
        assertEquals(2,              r.getComensales());
        assertEquals("CONFIRMADA",   r.getEstadoReserva());
        assertEquals("Cumpleaños",   r.getOcasion());
        assertEquals("Tarta sorpresa", r.getPeticionesEspeciales());
        assertEquals("Sin gluten",   r.getObservaciones());
    }

    @Test
    @DisplayName("Con constructor vacío el estado es null (lo asigna el DAO al leer de BD)")
    void estadoPorDefecto() {
        // El constructor vacío lo usa el DAO para mapear desde ResultSet
        // Por eso el estado empieza null — luego el DAO lo llena con setEstadoReserva
        Reserva r = new Reserva();
        assertNull(r.getEstadoReserva());
    }

    @Test
    @DisplayName("setComensales acepta cualquier valor positivo")
    void comensalesPositivos() {
        Reserva r = new Reserva();
        r.setComensales(8);
        assertEquals(8, r.getComensales());
    }

    @Test
    @DisplayName("getFechaReserva devuelve la misma fecha asignada")
    void fechaRoundTrip() {
        Reserva r = new Reserva();
        LocalDate hoy = LocalDate.now();
        r.setFechaReserva(hoy);
        assertEquals(hoy, r.getFechaReserva());
    }

    @Test
    @DisplayName("tieneOcasionEspecial devuelve false cuando es 'Ninguna' o null")
    void sinOcasionEspecial() {
        Reserva r1 = new Reserva();
        r1.setOcasion("Ninguna");
        assertFalse(r1.tieneOcasionEspecial());

        Reserva r2 = new Reserva();
        r2.setOcasion(null);
        assertFalse(r2.tieneOcasionEspecial());
    }

    @Test
    @DisplayName("tieneOcasionEspecial devuelve true con ocasiones válidas")
    void conOcasionEspecial() {
        Reserva r = new Reserva();
        r.setOcasion("Cumpleaños");
        assertTrue(r.tieneOcasionEspecial());

        r.setOcasion("Aniversario");
        assertTrue(r.tieneOcasionEspecial());
    }

    @Test
    @DisplayName("isEditable solo es true para CONFIRMADA, no para los otros tres estados")
    void estadoEditable() {
        Reserva r = new Reserva();

        r.setEstadoReserva("CONFIRMADA");
        assertTrue(r.isEditable(), "Una reserva confirmada sí se puede editar");

        // Los tres estados "cerrados" no deberían poder editarse
        r.setEstadoReserva("No presentado");
        assertFalse(r.isEditable());

        r.setEstadoReserva("CANCELADA");
        assertFalse(r.isEditable());

        r.setEstadoReserva("FINALIZADA");
        assertFalse(r.isEditable());
    }
}
