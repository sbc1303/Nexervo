package com.nexervo.modelo;

import javafx.beans.property.*;

/**
 * Modelo de presentación para mostrar reservas en la TableView del listado.
 * Combina datos de clientes, reservas y mesas mediante JOINs.
 */
public class ReservaView {
    private final IntegerProperty idReserva = new SimpleIntegerProperty();
    private final IntegerProperty idCliente  = new SimpleIntegerProperty();
    private final StringProperty  nombre     = new SimpleStringProperty();
    private final StringProperty  telefono   = new SimpleStringProperty();
    private final StringProperty  email      = new SimpleStringProperty();
    private final StringProperty  mesa       = new SimpleStringProperty();
    private final StringProperty  fecha      = new SimpleStringProperty();
    private final StringProperty  turno      = new SimpleStringProperty();
    private final StringProperty  personas   = new SimpleStringProperty();
    private final StringProperty  alergia    = new SimpleStringProperty();
    private final StringProperty  notas      = new SimpleStringProperty();

    public ReservaView(int idReserva, int idCliente, String nombre, String telefono,
                       String email, String mesa, String fecha, String turno,
                       String personas, String alergia, String notas) {
        this.idReserva.set(idReserva);
        this.idCliente.set(idCliente);
        this.nombre.set(nombre);
        this.telefono.set(telefono);
        this.email.set(email != null ? email : "");
        this.mesa.set(mesa);
        this.fecha.set(fecha);
        this.turno.set(turno);
        this.personas.set(personas);
        this.alergia.set(alergia);
        this.notas.set(notas != null ? notas : "");
    }

    // --- Properties ---
    public IntegerProperty idReservaProperty() { return idReserva; }
    public IntegerProperty idClienteProperty() { return idCliente; }
    public StringProperty  nombreProperty()    { return nombre; }
    public StringProperty  telefonoProperty()  { return telefono; }
    public StringProperty  emailProperty()     { return email; }
    public StringProperty  mesaProperty()      { return mesa; }
    public StringProperty  fechaProperty()     { return fecha; }
    public StringProperty  turnoProperty()     { return turno; }
    public StringProperty  personasProperty()  { return personas; }
    public StringProperty  alergiaProperty()   { return alergia; }
    public StringProperty  notasProperty()     { return notas; }

    // --- Getters simples ---
    public int    getIdReserva() { return idReserva.get(); }
    public int    getIdCliente() { return idCliente.get(); }
    public String getNombre()    { return nombre.get(); }
    public String getTelefono()  { return telefono.get(); }
    public String getEmail()     { return email.get(); }
    public String getMesa()      { return mesa.get(); }
    public String getFecha()     { return fecha.get(); }
    public String getTurno()     { return turno.get(); }
    public String getPersonas()  { return personas.get(); }
    public String getAlergia()   { return alergia.get(); }
    public String getNotas()     { return notas.get(); }
}
