package com.nexervo.modelo;

/**
 * Mapea la tabla 'intolerancias'.
 * Representa un alérgeno o intolerancia del catálogo.
 *
 * El campo 'tipo' permite al controlador agrupar los elementos
 * en dos secciones en el ComboBox:
 *   - ALERGENO_UE  → los 14 oficiales del Reglamento (UE) 1169/2011
 *   - INTOLERANCIA → intolerancias comunes no reguladas
 */
public class Intolerancia {

    private int    idIntolerancia;
    private String nombre;
    private String tipo;  // "ALERGENO_UE" | "INTOLERANCIA"

    // ── Constructores ────────────────────────────────────────────

    public Intolerancia() {}

    public Intolerancia(int idIntolerancia, String nombre, String tipo) {
        this.idIntolerancia = idIntolerancia;
        this.nombre         = nombre;
        this.tipo           = tipo;
    }

    // ── Getters y Setters ────────────────────────────────────────

    public int getIdIntolerancia()           { return idIntolerancia; }
    public void setIdIntolerancia(int v)     { this.idIntolerancia = v; }

    public String getNombre()                { return nombre; }
    public void setNombre(String v)          { this.nombre = v; }

    public String getTipo()                  { return tipo; }
    public void setTipo(String v)            { this.tipo = v; }

    /**
     * toString devuelve solo el nombre porque es lo que mostrará
     * el ComboBox automáticamente si se usa Intolerancia como tipo.
     */
    @Override
    public String toString() {
        return nombre;
    }
}
