package modelo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Mapea la tabla 'reservas' de la base de datos.
 * Es la entidad central del sistema: vincula un cliente con
 * una mesa en una fecha y hora concretas.
 */
public class Reserva {

    private int       idReserva;
    private int       idCliente;
    private int       idMesa;
    private LocalDate fechaReserva;
    private LocalTime horaReserva;
    private int       comensales;
    private String    estadoReserva;  // "CONFIRMADA" | "CANCELADA" | "FINALIZADA"
    private String    observaciones;  // notas libres de esta reserva concreta

    // Para mostrar en vistas (se rellena con JOIN en el DAO, no viene de la tabla reservas)
    private String nombreCliente;
    private String numeroMesa;

    // ── Constructores ────────────────────────────────────────────

    public Reserva() {}

    /** Constructor completo para crear una reserva nueva desde el formulario */
    public Reserva(int idCliente, int idMesa, LocalDate fechaReserva,
                   LocalTime horaReserva, int comensales, String observaciones) {
        this.idCliente    = idCliente;
        this.idMesa       = idMesa;
        this.fechaReserva = fechaReserva;
        this.horaReserva  = horaReserva;
        this.comensales   = comensales;
        this.estadoReserva = "CONFIRMADA";
        this.observaciones = observaciones;
    }

    // ── Getters y Setters ────────────────────────────────────────

    public int getIdReserva()               { return idReserva; }
    public void setIdReserva(int v)         { this.idReserva = v; }

    public int getIdCliente()               { return idCliente; }
    public void setIdCliente(int v)         { this.idCliente = v; }

    public int getIdMesa()                  { return idMesa; }
    public void setIdMesa(int v)            { this.idMesa = v; }

    public LocalDate getFechaReserva()      { return fechaReserva; }
    public void setFechaReserva(LocalDate v){ this.fechaReserva = v; }

    public LocalTime getHoraReserva()       { return horaReserva; }
    public void setHoraReserva(LocalTime v) { this.horaReserva = v; }

    public int getComensales()              { return comensales; }
    public void setComensales(int v)        { this.comensales = v; }

    public String getEstadoReserva()        { return estadoReserva; }
    public void setEstadoReserva(String v)  { this.estadoReserva = v; }

    public String getObservaciones()        { return observaciones; }
    public void setObservaciones(String v)  { this.observaciones = v; }

    public String getNombreCliente()        { return nombreCliente; }
    public void setNombreCliente(String v)  { this.nombreCliente = v; }

    public String getNumeroMesa()           { return numeroMesa; }
    public void setNumeroMesa(String v)     { this.numeroMesa = v; }

    @Override
    public String toString() {
        return String.format("Reserva{id=%d, cliente='%s', mesa='%s', fecha=%s, hora=%s, estado=%s}",
                idReserva, nombreCliente, numeroMesa, fechaReserva, horaReserva, estadoReserva);
    }
}
