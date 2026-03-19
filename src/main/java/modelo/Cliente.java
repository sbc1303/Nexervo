package modelo;

/**
 * Mapea la tabla 'clientes' de la base de datos.
 * El campo 'observaciones' es para notas libres del personal.
 * Los datos de reserva ya NO se almacenan aquí (van en la tabla reservas).
 */
public class Cliente {

    private int    idCliente;
    private String nombre;
    private String telefono;
    private String email;
    private String observaciones;

    // ── Constructores ────────────────────────────────────────────

    public Cliente() {}

    public Cliente(String nombre, String telefono, String email, String observaciones) {
        this.nombre        = nombre;
        this.telefono      = telefono;
        this.email         = email;
        this.observaciones = observaciones;
    }

    // ── Getters y Setters ────────────────────────────────────────

    public int getIdCliente()                { return idCliente; }
    public void setIdCliente(int v)          { this.idCliente = v; }

    public String getNombre()                { return nombre; }
    public void setNombre(String v)          { this.nombre = v; }

    public String getTelefono()              { return telefono; }
    public void setTelefono(String v)        { this.telefono = v; }

    public String getEmail()                 { return email; }
    public void setEmail(String v)           { this.email = v; }

    public String getObservaciones()         { return observaciones; }
    public void setObservaciones(String v)   { this.observaciones = v; }

    @Override
    public String toString() {
        return nombre + " (" + telefono + ")";
    }
}
