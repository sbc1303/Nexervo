package modelo;

/**
 * Mapea la tabla 'usuarios'.
 * Gestiona el acceso al sistema por rol.
 *
 * Roles disponibles:
 *   ADMIN    → acceso total: gestión de mesas, usuarios, informes y borrados
 *   EMPLEADO → acceso operativo: alta/consulta/cancelación de reservas
 *
 * La contraseña NUNCA se guarda en texto plano en producción.
 * En esta fase de desarrollo se usa texto plano para simplificar.
 * En fase final se sustituirá por hash BCrypt.
 */
public class Usuario {

    private int     idUsuario;
    private String  nombre;
    private String  usuario;      // login
    private String  contrasena;   // en memoria solo, nunca se persiste de vuelta
    private String  rol;          // "ADMIN" | "EMPLEADO"
    private boolean activo;

    // ── Constructores ────────────────────────────────────────────

    public Usuario() {}

    public Usuario(String nombre, String usuario, String contrasena, String rol) {
        this.nombre     = nombre;
        this.usuario    = usuario;
        this.contrasena = contrasena;
        this.rol        = rol;
        this.activo     = true;
    }

    // ── Getters y Setters ────────────────────────────────────────

    public int getIdUsuario()           { return idUsuario; }
    public void setIdUsuario(int v)     { this.idUsuario = v; }

    public String getNombre()           { return nombre; }
    public void setNombre(String v)     { this.nombre = v; }

    public String getUsuario()          { return usuario; }
    public void setUsuario(String v)    { this.usuario = v; }

    public String getContrasena()       { return contrasena; }
    public void setContrasena(String v) { this.contrasena = v; }

    public String getRol()              { return rol; }
    public void setRol(String v)        { this.rol = v; }

    public boolean isActivo()           { return activo; }
    public void setActivo(boolean v)    { this.activo = v; }

    /** Comprueba si el usuario tiene rol de administrador */
    public boolean esAdmin() {
        return "ADMIN".equals(this.rol);
    }

    @Override
    public String toString() {
        return String.format("Usuario{login='%s', rol=%s, activo=%s}", usuario, rol, activo);
    }
}
