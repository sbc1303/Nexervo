package com.nexervo.modelo;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Clase modelo para la tabla 'reservas'.
 * Vincula un cliente con una mesa en fecha y hora concretas.
 *
 * Al principio solo tenía los campos básicos (mesa, fecha, hora, comensales)
 * pero mi tutor me dijo que en la vida real siempre se apunta mucho más:
 * si es un cumpleaños, si hay algún alérgico, peticiones especiales...
 * Con mi experiencia en sala lo vi claro y añadí la ocasión y las peticiones.
 *
 * También añadí el estado NO_PRESENTADO después de darme cuenta de que CANCELADA
 * no es lo mismo que no presentarse — un cancelado da tiempo a rellenar la mesa,
 * un no se presentó te deja con la mesa vacía sin aviso.
 */
public class Reserva {

    private int       idReserva;
    private int       idCliente;
    private int       idMesa;
    private LocalDate fechaReserva;
    private LocalTime horaReserva;
    private int       comensales;
    private String    estadoReserva;        // "CONFIRMADA" | "CANCELADA" | "FINALIZADA" | "No presentado"
    private String    ocasion;              // "Ninguna" | "Cumpleaños" | "Aniversario" | etc.
    private String    peticionesEspeciales; // silla bebé, acceso PMR, decoración, etc.
    private String    observaciones;        // notas libres del personal

    // Para mostrar en vistas (se rellena con JOIN en el DAO)
    private String nombreCliente;
    private String numeroMesa;

    // Los valores de ocasión los centralizo aquí para no repetirlos en cada formulario
    // (antes los tenía hardcoded en dos sitios distintos y dejaban de coincidir)
    public static final String[] OCASIONES = {
        "Ninguna",
        "Cumpleaños",
        "Aniversario",
        "Primera Comunión",
        "Boda / Celebración",
        "Cena de empresa",
        "Reunión de negocios",
        "Despedida de soltero/a",
        "Otras"
    };

    // ── Constructores ────────────────────────────────────────────

    public Reserva() {}

    /**
     * Constructor para crear una reserva nueva desde el formulario.
     * El estado siempre empieza como CONFIRMADA — el resto de estados
     * los gestiona el personal desde GestionReservas.
     */
    public Reserva(int idCliente, int idMesa, LocalDate fechaReserva,
                   LocalTime horaReserva, int comensales,
                   String ocasion, String peticionesEspeciales, String observaciones) {
        this.idCliente           = idCliente;
        this.idMesa              = idMesa;
        this.fechaReserva        = fechaReserva;
        this.horaReserva         = horaReserva;
        this.comensales          = comensales;
        this.estadoReserva       = "CONFIRMADA"; // siempre empieza confirmada
        this.ocasion             = ocasion != null ? ocasion : "Ninguna";
        this.peticionesEspeciales = peticionesEspeciales;
        this.observaciones       = observaciones;
    }

    // ── Getters y Setters ────────────────────────────────────────

    public int getIdReserva()                         { return idReserva; }
    public void setIdReserva(int v)                   { this.idReserva = v; }

    public int getIdCliente()                         { return idCliente; }
    public void setIdCliente(int v)                   { this.idCliente = v; }

    public int getIdMesa()                            { return idMesa; }
    public void setIdMesa(int v)                      { this.idMesa = v; }

    public LocalDate getFechaReserva()                { return fechaReserva; }
    public void setFechaReserva(LocalDate v)          { this.fechaReserva = v; }

    public LocalTime getHoraReserva()                 { return horaReserva; }
    public void setHoraReserva(LocalTime v)           { this.horaReserva = v; }

    public int getComensales()                        { return comensales; }
    public void setComensales(int v)                  { this.comensales = v; }

    public String getEstadoReserva()                  { return estadoReserva; }
    public void setEstadoReserva(String v)            { this.estadoReserva = v; }

    public String getOcasion()                        { return ocasion != null ? ocasion : "Ninguna"; }
    public void setOcasion(String v)                  { this.ocasion = v; }

    public String getPeticionesEspeciales()       { return peticionesEspeciales; }
    public void setPeticionesEspeciales(String v) { this.peticionesEspeciales = v; }

    public String getObservaciones()                  { return observaciones; }
    public void setObservaciones(String v)            { this.observaciones = v; }

    public String getNombreCliente()                  { return nombreCliente; }
    public void setNombreCliente(String v)            { this.nombreCliente = v; }

    public String getNumeroMesa()                     { return numeroMesa; }
    public void setNumeroMesa(String v)               { this.numeroMesa = v; }

    // Solo se pueden editar reservas CONFIRMADAS — si ya está cancelada o finalizada no tiene sentido
    public boolean isEditable() { return "CONFIRMADA".equals(estadoReserva); }

    /**
     * Devuelve true si hay una ocasión especial que el equipo de sala debe preparar.
     * "Ninguna" y null se tratan igual (no hay ocasión).
     */
    public boolean tieneOcasionEspecial() {
        return ocasion != null && !ocasion.isBlank() && !"Ninguna".equals(ocasion);
    }

    @Override
    public String toString() {
        return String.format("Reserva{id=%d, cliente='%s', mesa='%s', fecha=%s, hora=%s, estado=%s, ocasion=%s}",
                idReserva, nombreCliente, numeroMesa, fechaReserva, horaReserva, estadoReserva, ocasion);
    }
}
