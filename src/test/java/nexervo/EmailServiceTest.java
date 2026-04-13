package nexervo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.nexervo.servicio.EmailService;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios de EmailService.
 *
 * No se realiza ningún envío real. Los tests comprueban:
 *   1. El comportamiento con email deshabilitado (sin config.properties de producción).
 *   2. Los casos de entrada inválida (destinatario nulo o vacío).
 *   3. El contenido HTML generado para la confirmación de reserva.
 *
 * Como config.properties no existe en el classpath de test (solo existe en
 * resources de producción), la clase carga CONFIG vacío y por tanto
 * email.habilitado siempre será "false" durante estos tests — lo que es
 * exactamente el comportamiento correcto para no intentar abrir SMTP en CI.
 */
@DisplayName("EmailService — lógica de envío y generación HTML")
class EmailServiceTest {

    // ── Casos de retorno rápido (early-return) ────────────────────────

    @Test
    @DisplayName("Con email deshabilitado (sin config) devuelve false sin intentar conectar")
    void enviarConfirmacion_deshabilitado_devuelveFalse() {
        // No hay config.properties en el classpath de test → email.habilitado no está → false
        boolean resultado = EmailService.enviarConfirmacion(
            "cliente@ejemplo.com",
            "Ana García",
            42,
            "M3",
            LocalDate.of(2026, 6, 15),
            LocalTime.of(21, 0),
            4,
            "40.00"
        );
        assertFalse(resultado, "Debe devolver false cuando email.habilitado no es 'true'");
    }

    @Test
    @DisplayName("Con destinatario nulo devuelve false")
    void enviarConfirmacion_destinatarioNulo_devuelveFalse() {
        boolean resultado = EmailService.enviarConfirmacion(
            null,
            "Pedro López",
            10,
            "M1",
            LocalDate.now(),
            LocalTime.of(14, 0),
            2,
            "20.00"
        );
        assertFalse(resultado, "No se puede enviar email sin destinatario");
    }

    @Test
    @DisplayName("Con destinatario vacío devuelve false")
    void enviarConfirmacion_destinatarioVacio_devuelveFalse() {
        boolean resultado = EmailService.enviarConfirmacion(
            "   ",
            "Pedro López",
            10,
            "M1",
            LocalDate.now(),
            LocalTime.of(14, 0),
            2,
            "20.00"
        );
        assertFalse(resultado, "Un destinatario en blanco debe tratarse igual que nulo");
    }

    // ── Contenido HTML generado ───────────────────────────────────────

    /**
     * Accede al método privado construirHtml mediante reflexión para
     * comprobar que el HTML incluye los datos de la reserva.
     * Esta técnica es habitual en tests de caja blanca.
     */
    @Test
    @DisplayName("El HTML de confirmación contiene el número de reserva")
    void htmlContieneNumeroReserva() throws Exception {
        String html = invocarConstruirHtml("María Jiménez", 99, "M5",
                LocalDate.of(2026, 7, 1), LocalTime.of(20, 30), 3, "30.00");

        assertTrue(html.contains("#99"),
                "El HTML debe incluir el número de reserva precedido de #");
    }

    @Test
    @DisplayName("El HTML de confirmación contiene la mesa")
    void htmlContieneMesa() throws Exception {
        String html = invocarConstruirHtml("Juan Ruiz", 5, "M7",
                LocalDate.of(2026, 8, 1), LocalTime.of(14, 0), 2, "20.00");

        assertTrue(html.contains("M7"), "El HTML debe incluir el número de mesa");
    }

    @Test
    @DisplayName("El HTML usa solo el primer nombre del cliente")
    void htmlUsaNombreCorto() throws Exception {
        String html = invocarConstruirHtml("Carlos Fernández Ruiz", 1, "M1",
                LocalDate.of(2026, 9, 1), LocalTime.of(21, 0), 4, "40.00");

        assertTrue(html.contains("Carlos"),   "Debe aparecer el primer nombre");
        assertFalse(html.contains("Fernández Ruiz"),
                "No debe aparecer el apellido en el saludo principal");
    }

    @Test
    @DisplayName("El HTML incluye el importe de preautorización")
    void htmlContieneImporte() throws Exception {
        String html = invocarConstruirHtml("Laura Martín", 7, "M2",
                LocalDate.of(2026, 10, 1), LocalTime.of(20, 0), 5, "50.00");

        assertTrue(html.contains("50.00"),
                "El HTML debe mostrar el importe de la preautorización");
    }

    @Test
    @DisplayName("El HTML incluye la fecha formateada correctamente (dd/MM/yyyy)")
    void htmlContieneFechaFormateada() throws Exception {
        String html = invocarConstruirHtml("Luis Sanz", 3, "M4",
                LocalDate.of(2026, 12, 25), LocalTime.of(14, 30), 2, "20.00");

        assertTrue(html.contains("25/12/2026"),
                "La fecha debe aparecer en formato dd/MM/yyyy");
    }

    @Test
    @DisplayName("El HTML incluye el aviso de cancelación con 24 horas")
    void htmlContieneAvisoCancelacion() throws Exception {
        String html = invocarConstruirHtml("Rosa Gil", 15, "M3",
                LocalDate.of(2027, 1, 1), LocalTime.of(21, 0), 2, "20.00");

        assertTrue(html.toLowerCase().contains("24 horas"),
                "El aviso de cancelación debe indicar las 24 horas de antelación");
    }

    // ── Utilidad ─────────────────────────────────────────────────────

    /**
     * Invoca el método privado estático EmailService#construirHtml mediante reflexión.
     */
    private String invocarConstruirHtml(String nombre, int idReserva, String mesa,
                                         LocalDate fecha, LocalTime hora,
                                         int comensales, String importe) throws Exception {
        Method m = EmailService.class.getDeclaredMethod(
            "construirHtml",
            String.class, int.class, String.class,
            LocalDate.class, LocalTime.class, int.class, String.class
        );
        m.setAccessible(true);
        return (String) m.invoke(null, nombre, idReserva, mesa, fecha, hora, comensales, importe);
    }
}
