package com.nexervo.servicio;

import com.nexervo.datos.PagoDAO;
import com.nexervo.datos.ReservaDAO;
import com.nexervo.modelo.Reserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Capa de servicio para operaciones sobre reservas.
 *
 * El controlador no debería hablar directamente con los DAOs porque
 * hay lógica de negocio que no tiene sentido en la vista (comprobar
 * conflictos antes de guardar, crear la preautorización automáticamente, etc.).
 * Esta clase centraliza esa lógica.
 *
 * Los controladores que ya usaban ReservaDAO directamente han sido
 * migrados para usar esta capa de servicio (GestionReservasControlador
 * usa obtenerReservasDelDia() y hayConflicto() desde esta clase).
 */
public class ReservaServicio {

    private static final Logger log = LoggerFactory.getLogger(ReservaServicio.class);

    // Importe de preautorización por comensal (regla de negocio del restaurante)
    private static final BigDecimal IMPORTE_POR_PAX = new BigDecimal("10.00");

    private final ReservaDAO reservaDAO;
    private final PagoDAO    pagoDAO;

    public ReservaServicio() {
        this.reservaDAO = new ReservaDAO();
        this.pagoDAO    = new PagoDAO();
    }

    /** Constructor para tests, permite inyectar DAOs mock o con H2 */
    public ReservaServicio(ReservaDAO reservaDAO, PagoDAO pagoDAO) {
        this.reservaDAO = reservaDAO;
        this.pagoDAO    = pagoDAO;
    }

    /**
     * Crea una reserva validando antes que no haya conflicto de mesa.
     * Si todo va bien, crea también la preautorización de pago.
     *
     * @return el id de la reserva creada, o -1 si hay conflicto o fallo de BD
     */
    public int crearReservaConPago(Reserva reserva) {
        // Primero comprobar que la mesa está libre a esa hora
        if (reservaDAO.hayConflicto(reserva.getIdMesa(),
                                    reserva.getFechaReserva(),
                                    reserva.getHoraReserva(), -1)) {
            log.warn("Conflicto al crear reserva: mesa {} ocupada el {} a las {}",
                     reserva.getIdMesa(),
                     reserva.getFechaReserva(),
                     reserva.getHoraReserva());
            return -1;
        }

        int idReserva = reservaDAO.crearReserva(reserva);
        if (idReserva < 0) {
            log.error("No se pudo insertar la reserva en BD");
            return -1;
        }

        // Calcular el importe y crear la preautorización
        BigDecimal importe = IMPORTE_POR_PAX.multiply(new BigDecimal(reserva.getComensales()));
        int idPago = pagoDAO.crearPreautorizacion(idReserva, importe);
        if (idPago < 0) {
            // La reserva se creó pero la preautorización falló.
            // Registro el error pero no deshago la reserva — se puede gestionar manualmente.
            log.error("Reserva #{} creada pero falló la preautorización de pago", idReserva);
        }

        log.info("Reserva #{} creada: mesa {}, {} pax, {}", idReserva,
                 reserva.getIdMesa(), reserva.getComensales(), reserva.getFechaReserva());
        return idReserva;
    }

    /**
     * Cancela una reserva y libera la preautorización de pago si existe.
     */
    public boolean cancelarReserva(int idReserva) {
        // Buscar la preautorización para devolverla
        var pago = pagoDAO.obtenerPorReserva(idReserva);
        if (pago != null && "PENDIENTE".equals(pago.getEstado())) {
            pagoDAO.cambiarEstado(pago.getIdPago(), "DEVUELTA");
            log.info("Preautorización #{} devuelta al cancelar reserva #{}", pago.getIdPago(), idReserva);
        }

        boolean ok = reservaDAO.cambiarEstado(idReserva, "CANCELADA");
        if (ok) log.info("Reserva #{} cancelada", idReserva);
        return ok;
    }

    /**
     * Devuelve todas las reservas de una fecha, ordenadas por hora.
     * Usado por la hoja del día y el dashboard.
     */
    public List<Reserva> obtenerReservasDelDia(LocalDate fecha) {
        return reservaDAO.obtenerPorFecha(fecha);
    }

    /**
     * Historial de reservas de un cliente (más reciente primero).
     */
    public List<Reserva> historialCliente(int idCliente) {
        return reservaDAO.obtenerPorCliente(idCliente);
    }

    /**
     * Comprueba si hay conflicto para una mesa/fecha/hora.
     * El parámetro excludeId sirve para editar una reserva existente
     * (se excluye a sí misma de la comprobación).
     */
    public boolean hayConflicto(int idMesa, LocalDate fecha, LocalTime hora, int excludeId) {
        return reservaDAO.hayConflicto(idMesa, fecha, hora, excludeId);
    }

    /** Calcula el importe de preautorización para N comensales */
    public BigDecimal calcularImporte(int comensales) {
        return IMPORTE_POR_PAX.multiply(new BigDecimal(comensales));
    }
}
