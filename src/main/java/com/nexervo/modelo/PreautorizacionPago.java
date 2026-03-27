package com.nexervo.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mapea la tabla 'preautorizaciones_pago'.
 *
 * Ciclo de estados:
 *   PENDIENTE → AUTORIZADA → DEVUELTA  (servicio completado correctamente)
 *   PENDIENTE → RECHAZADA              (pago denegado en simulación)
 *   CONFIRMADA → CANCELADA → DEVUELTA  (no se presentó o cancelación tardía)
 *
 * No se integra con pasarelas reales. Ver memoria, apartado 2.3.2.
 */
public class PreautorizacionPago {

    private int           idPago;
    private int           idReserva;
    private BigDecimal    importe;
    private String        estado;         // "PENDIENTE" | "AUTORIZADA" | "RECHAZADA" | "DEVUELTA"
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaGestion;   // cuando cambia de PENDIENTE a otro estado

    // ── Constructores ────────────────────────────────────────────

    public PreautorizacionPago() {}

    /** Constructor para crear una preautorización nueva al confirmar una reserva */
    public PreautorizacionPago(int idReserva, BigDecimal importe) {
        this.idReserva = idReserva;
        this.importe   = importe;
        this.estado    = "PENDIENTE";
    }

    // ── Getters y Setters ────────────────────────────────────────

    public int getIdPago()                        { return idPago; }
    public void setIdPago(int v)                  { this.idPago = v; }

    public int getIdReserva()                     { return idReserva; }
    public void setIdReserva(int v)               { this.idReserva = v; }

    public BigDecimal getImporte()                { return importe; }
    public void setImporte(BigDecimal v)          { this.importe = v; }

    public String getEstado()                     { return estado; }
    public void setEstado(String v)               { this.estado = v; }

    public LocalDateTime getFechaCreacion()       { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime v) { this.fechaCreacion = v; }

    public LocalDateTime getFechaGestion()        { return fechaGestion; }
    public void setFechaGestion(LocalDateTime v)  { this.fechaGestion = v; }

    @Override
    public String toString() {
        return String.format("Pago{id=%d, reserva=%d, importe=%.2f€, estado=%s}",
                idPago, idReserva, importe, estado);
    }
}
