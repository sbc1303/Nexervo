package com.nexervo.servicio;

import com.nexervo.modelo.Reserva;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.awt.Color;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Genera el PDF de la "Hoja del día" (parte de sala).
 *
 * Usa Apache PDFBox 3.x. El diseño imita la estética de NEXERVO:
 * fondo oscuro en la cabecera, texto en blanco, tabla con filas alternas.
 * El PDF resultante se puede imprimir o enviar al cocinero.
 *
 * Método principal: generarHojaDelDia(), devuelve la ruta del archivo guardado.
 *
 * Añadido en v4 como mejora funcional del TFC (mejora prevista en README).
 */
public class PdfService {

    // Paleta de colores NEXERVO
    private static final Color COLOR_FONDO_HEADER = new Color(10, 5, 32);
    private static final Color COLOR_CIAN         = new Color(0, 198, 255);
    private static final Color COLOR_MORADO       = new Color(42, 26, 110);
    private static final Color COLOR_TEXTO_CLARO  = new Color(232, 232, 240);
    private static final Color COLOR_TEXTO_GRIS   = new Color(144, 144, 187);
    private static final Color COLOR_FILA_PAR     = new Color(15, 10, 50);
    private static final Color COLOR_FILA_IMPAR   = new Color(22, 34, 82);
    private static final Color COLOR_VERDE        = new Color(0, 230, 118);
    private static final Color COLOR_DORADO       = new Color(255, 193, 7);

    private static final DateTimeFormatter FMT_FECHA =
        DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", new Locale("es"));
    private static final DateTimeFormatter FMT_ARCHIVO =
        DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Genera el PDF de la hoja del día y lo guarda en la carpeta de descargas del usuario.
     *
     * @param fecha     Fecha del servicio
     * @param comidas   Reservas del turno de comida (confirmadas)
     * @param cenas     Reservas del turno de cena (confirmadas)
     * @param totalPax  Total de cubiertos del día
     * @return Ruta absoluta del archivo generado
     * @throws IOException si no puede escribir el archivo
     */
    public static String generarHojaDelDia(LocalDate fecha,
                                            List<Reserva> comidas,
                                            List<Reserva> cenas,
                                            long totalPax) throws IOException {

        String rutaSalida = System.getProperty("user.home")
            + "/Downloads/hoja_dia_" + fecha.format(FMT_ARCHIVO) + ".pdf";

        try (PDDocument doc = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            doc.addPage(pagina);

            PDType1Font fuenteNormal    = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDType1Font fuenteNegrita   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fuenteMono      = new PDType1Font(Standard14Fonts.FontName.COURIER);
            PDType1Font fuenteMonoNeg   = new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);

            float ancho  = pagina.getMediaBox().getWidth();
            float alto   = pagina.getMediaBox().getHeight();
            float margen = 40f;
            float y      = alto - margen;

            try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {

                // ── CABECERA ─────────────────────────────────────────
                cs.setNonStrokingColor(COLOR_FONDO_HEADER);
                cs.addRect(0, alto - 90, ancho, 90);
                cs.fill();

                // Línea cian bajo la cabecera
                cs.setStrokingColor(COLOR_CIAN);
                cs.setLineWidth(2f);
                cs.moveTo(0, alto - 90);
                cs.lineTo(ancho, alto - 90);
                cs.stroke();

                // Título NEXERVO
                cs.beginText();
                cs.setFont(fuenteNegrita, 26);
                cs.setNonStrokingColor(COLOR_CIAN);
                cs.newLineAtOffset(margen, alto - 40);
                cs.showText("NEXERVO");
                cs.endText();

                // Subtítulo
                cs.beginText();
                cs.setFont(fuenteNormal, 10);
                cs.setNonStrokingColor(COLOR_TEXTO_GRIS);
                cs.newLineAtOffset(margen, alto - 58);
                cs.showText("PARTE DE SALA — HOJA DEL DÍA");
                cs.endText();

                // Fecha en la cabecera (derecha)
                String fechaStr = fecha.format(FMT_FECHA).toUpperCase();
                cs.beginText();
                cs.setFont(fuenteNormal, 9);
                cs.setNonStrokingColor(COLOR_TEXTO_GRIS);
                float anchoFecha = fuenteNormal.getStringWidth(fechaStr) / 1000 * 9;
                cs.newLineAtOffset(ancho - margen - anchoFecha, alto - 52);
                cs.showText(fechaStr);
                cs.endText();

                y = alto - 110;

                // ── TURNO COMIDA ──────────────────────────────────────
                if (!comidas.isEmpty()) {
                    y = dibujarSeccion(cs, doc, pagina, "TURNO DE COMIDA",
                                       comidas, fuenteNegrita, fuenteNormal,
                                       fuenteMono, fuenteMonoNeg,
                                       ancho, margen, y);
                    y -= 16;
                }

                // ── TURNO CENA ────────────────────────────────────────
                if (!cenas.isEmpty()) {
                    y = dibujarSeccion(cs, doc, pagina, "TURNO DE CENA",
                                       cenas, fuenteNegrita, fuenteNormal,
                                       fuenteMono, fuenteMonoNeg,
                                       ancho, margen, y);
                    y -= 16;
                }

                // ── RESUMEN TOTAL ─────────────────────────────────────
                int totalMesas = comidas.size() + cenas.size();
                String resumen = String.format(
                    "TOTAL: %d mesa(s)  ·  %d cubierto(s)", totalMesas, totalPax);

                cs.setNonStrokingColor(COLOR_MORADO);
                cs.addRect(margen, y - 22, ancho - 2 * margen, 30);
                cs.fill();

                cs.beginText();
                cs.setFont(fuenteNegrita, 11);
                cs.setNonStrokingColor(COLOR_CIAN);
                cs.newLineAtOffset(margen + 10, y - 10);
                cs.showText(resumen);
                cs.endText();

                // ── PIE ───────────────────────────────────────────────
                cs.beginText();
                cs.setFont(fuenteNormal, 8);
                cs.setNonStrokingColor(COLOR_TEXTO_GRIS);
                cs.newLineAtOffset(margen, 25);
                cs.showText("NEXERVO · Sistema de Gestión de Reservas  —  Generado automáticamente");
                cs.endText();
            }

            doc.save(rutaSalida);
        }

        return rutaSalida;
    }

    /**
     * Dibuja la sección de un turno (COMIDA o CENA) con su tabla de reservas.
     * Devuelve la coordenada Y tras el último elemento dibujado.
     */
    private static float dibujarSeccion(PDPageContentStream cs, PDDocument doc,
                                         PDPage pagina, String titulo,
                                         List<Reserva> reservas,
                                         PDType1Font fNeg, PDType1Font fNorm,
                                         PDType1Font fMono, PDType1Font fMonoNeg,
                                         float ancho, float margen, float y)
            throws IOException {

        // Cabecera de sección
        cs.setNonStrokingColor(COLOR_MORADO);
        cs.addRect(margen, y - 18, ancho - 2 * margen, 22);
        cs.fill();

        cs.beginText();
        cs.setFont(fNeg, 10);
        cs.setNonStrokingColor(COLOR_CIAN);
        cs.newLineAtOffset(margen + 8, y - 10);
        cs.showText(titulo);
        cs.endText();

        y -= 26;

        // Cabeceras de columna
        float[] xCols = { margen + 5, margen + 55, margen + 130, margen + 230, margen + 305 };
        String[] cabeceras = { "HORA", "MESA", "CLIENTE", "PAX", "OCASIÓN" };

        cs.setNonStrokingColor(new Color(26, 10, 60));
        cs.addRect(margen, y - 14, ancho - 2 * margen, 18);
        cs.fill();

        for (int i = 0; i < cabeceras.length; i++) {
            cs.beginText();
            cs.setFont(fNeg, 8);
            cs.setNonStrokingColor(COLOR_TEXTO_GRIS);
            cs.newLineAtOffset(xCols[i], y - 8);
            cs.showText(cabeceras[i]);
            cs.endText();
        }
        y -= 20;

        // Filas de reservas
        for (int i = 0; i < reservas.size(); i++) {
            Reserva r = reservas.get(i);
            Color colorFila = (i % 2 == 0) ? COLOR_FILA_PAR : COLOR_FILA_IMPAR;
            float alturaFila = 16f;

            // Fila con ocasión o alerta → un poco más alta
            boolean tieneExtra = r.tieneOcasionEspecial()
                || (r.getPeticionesEspeciales() != null && !r.getPeticionesEspeciales().isBlank());
            if (tieneExtra) alturaFila = 26f;

            cs.setNonStrokingColor(colorFila);
            cs.addRect(margen, y - alturaFila, ancho - 2 * margen, alturaFila);
            cs.fill();

            // Datos de la fila
            String hora     = r.getHoraReserva().toString();
            String mesa     = r.getNumeroMesa();
            String cliente  = truncar(r.getNombreCliente(), 22);
            String pax      = String.valueOf(r.getComensales());
            String ocasion  = r.getOcasion() != null ? r.getOcasion() : "—";

            String[] datos = { hora, mesa, cliente, pax, ocasion };
            for (int j = 0; j < datos.length; j++) {
                cs.beginText();
                cs.setFont(fMono, 8);
                cs.setNonStrokingColor(COLOR_TEXTO_CLARO);
                cs.newLineAtOffset(xCols[j], y - 10);
                cs.showText(datos[j]);
                cs.endText();
            }

            // Segunda línea si hay petición especial
            if (tieneExtra && r.getPeticionesEspeciales() != null
                           && !r.getPeticionesEspeciales().isBlank()) {
                cs.beginText();
                cs.setFont(fNorm, 7);
                cs.setNonStrokingColor(COLOR_DORADO);
                cs.newLineAtOffset(xCols[0], y - 20);
                cs.showText("Peticion: " + truncar(r.getPeticionesEspeciales(), 60));
                cs.endText();
            }

            y -= alturaFila;

            // Línea separadora entre filas
            cs.setStrokingColor(COLOR_MORADO);
            cs.setLineWidth(0.3f);
            cs.moveTo(margen, y);
            cs.lineTo(ancho - margen, y);
            cs.stroke();
        }

        return y;
    }

    private static String truncar(String texto, int max) {
        if (texto == null) return "—";
        return texto.length() <= max ? texto : texto.substring(0, max - 1) + "…";
    }
}
