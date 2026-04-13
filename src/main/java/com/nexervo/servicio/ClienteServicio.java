package com.nexervo.servicio;

import com.nexervo.datos.ClienteDAO;
import com.nexervo.datos.ReservaDAO;
import com.nexervo.modelo.Cliente;
import com.nexervo.modelo.Intolerancia;
import com.nexervo.modelo.Reserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Lógica de negocio relacionada con clientes.
 *
 * Separo esto del DAO porque hay operaciones que combinan
 * ClienteDAO y ReservaDAO, y ponerlas en el controlador
 * mezclaba demasiado la UI con la lógica.
 *
 * Por ejemplo, buscarORegistrar() primero mira si el cliente
 * ya existe por teléfono y si no, lo crea — eso era código
 * duplicado en varios controladores.
 */
public class ClienteServicio {

    private static final Logger log = LoggerFactory.getLogger(ClienteServicio.class);

    private final ClienteDAO clienteDAO;
    private final ReservaDAO reservaDAO;

    public ClienteServicio() {
        this.clienteDAO = new ClienteDAO();
        this.reservaDAO = new ReservaDAO();
    }

    /** Constructor para tests */
    public ClienteServicio(ClienteDAO clienteDAO, ReservaDAO reservaDAO) {
        this.clienteDAO = clienteDAO;
        this.reservaDAO = reservaDAO;
    }

    /**
     * Busca un cliente por teléfono. Si no existe, lo registra nuevo.
     * Devuelve el cliente con su id_cliente relleno.
     *
     * Útil al crear una reserva: no hace falta que el recepcionista
     * vaya primero a la pantalla de clientes para darlo de alta.
     */
    public Cliente buscarORegistrar(String nombre, String telefono, String email) {
        Cliente existente = clienteDAO.buscarPorTelefono(telefono);
        if (existente != null) {
            log.debug("Cliente encontrado por teléfono {}: id={}", telefono, existente.getIdCliente());
            return existente;
        }

        // No existe, lo creamos
        Cliente nuevo = new Cliente(nombre, telefono, email, null);
        int id = clienteDAO.registrarCliente(nuevo);
        if (id < 0) {
            log.error("No se pudo registrar el cliente con teléfono {}", telefono);
            return null;
        }
        nuevo.setIdCliente(id);
        log.info("Cliente nuevo registrado: {} (id={})", nombre, id);
        return nuevo;
    }

    /**
     * Genera un resumen de las visitas de un cliente para mostrar
     * en el panel de "cliente habitual" al crear una reserva.
     *
     * Devuelve null si el cliente no tiene reservas previas.
     */
    public String resumenHistorial(int idCliente) {
        List<Reserva> historial = reservaDAO.obtenerPorCliente(idCliente);

        // Filtrar solo las finalizadas — las canceladas no cuentan como visitas
        long visitas = historial.stream()
                .filter(r -> "FINALIZADA".equals(r.getEstadoReserva()))
                .count();

        if (visitas == 0) return null;

        // La más reciente está primera (ORDER BY fecha DESC en el DAO)
        Reserva ultima = historial.stream()
                .filter(r -> "FINALIZADA".equals(r.getEstadoReserva()))
                .findFirst()
                .orElse(null);

        if (ultima == null) return null;

        return String.format("%d visita%s · última el %s (mesa %s)",
                visitas,
                visitas == 1 ? "" : "s",
                ultima.getFechaReserva(),
                ultima.getNumeroMesa() != null ? ultima.getNumeroMesa() : "—");
    }

    public List<Intolerancia> intoleranciasDe(int idCliente) {
        return clienteDAO.obtenerIntoleranciasPorCliente(idCliente);
    }

    public List<Cliente> listarTodos() {
        return clienteDAO.listarClientes();
    }
}
