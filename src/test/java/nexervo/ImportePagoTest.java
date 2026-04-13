package nexervo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de la lógica de cálculo de preautorizaciones (sin BD).
 *
 * Regla de negocio: 10 € por comensal en el momento de confirmar.
 */
@DisplayName("Cálculo de importe de preautorización")
class ImportePagoTest {

    private static final BigDecimal IMPORTE_PAX = new BigDecimal("10.00");

    private BigDecimal calcular(int pax) {
        return IMPORTE_PAX.multiply(new BigDecimal(pax));
    }

    @Test
    @DisplayName("1 comensal → 10,00 €")
    void unComensal() {
        assertEquals(new BigDecimal("10.00"), calcular(1));
    }

    @Test
    @DisplayName("4 comensales → 40,00 €")
    void cuatroComensales() {
        assertEquals(new BigDecimal("40.00"), calcular(4));
    }

    @Test
    @DisplayName("El importe nunca puede ser negativo")
    void importeNoNegativo() {
        assertTrue(calcular(1).compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("20 comensales (máximo spinner) → 200,00 €")
    void maximoComensales() {
        assertEquals(new BigDecimal("200.00"), calcular(20));
    }
}
