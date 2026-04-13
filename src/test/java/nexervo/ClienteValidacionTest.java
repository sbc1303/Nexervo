package nexervo;

import com.nexervo.modelo.Cliente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de validación del modelo Cliente y las reglas de negocio
 * que aplican sin necesidad de BD.
 */
@DisplayName("Validaciones de Cliente")
class ClienteValidacionTest {

    /** Simula la misma validación que hace ClientesControlador.onGuardar() */
    private boolean esValido(String nombre, String telefono) {
        return nombre != null && !nombre.isBlank()
            && telefono != null && !telefono.isBlank();
    }

    @Test
    @DisplayName("Cliente con nombre y teléfono es válido")
    void clienteValido() {
        assertTrue(esValido("Ana García", "612345678"));
    }

    @Test
    @DisplayName("Nombre vacío → inválido")
    void nombreVacio() {
        assertFalse(esValido("", "612345678"));
    }

    @Test
    @DisplayName("Teléfono vacío → inválido")
    void telefonoVacio() {
        assertFalse(esValido("Ana García", ""));
    }

    @Test
    @DisplayName("Ambos campos vacíos → inválido")
    void ambosVacios() {
        assertFalse(esValido("", ""));
    }

    @Test
    @DisplayName("El modelo Cliente almacena y devuelve los datos correctamente")
    void clienteRoundTrip() {
        Cliente c = new Cliente("Pedro Martínez", "666777888", "pedro@mail.com", "VIP");
        assertEquals("Pedro Martínez", c.getNombre());
        assertEquals("666777888",      c.getTelefono());
        assertEquals("pedro@mail.com", c.getEmail());
        assertEquals("VIP",            c.getObservaciones());
    }
}
