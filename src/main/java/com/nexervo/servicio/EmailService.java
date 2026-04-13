package com.nexervo.servicio;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Envía el email de confirmación cuando se crea una reserva.
 *
 * Lee la configuración de config.properties. Para la contraseña,
 * primero mira la variable de entorno NEXERVO_EMAIL_CLAVE y solo
 * si no la encuentra usa el valor de config.properties — así
 * en producción no hace falta poner la clave en el fichero.
 *
 * Para Gmail: hay que tener activada la verificación en 2 pasos
 * y generar una "contraseña de aplicación" en la cuenta de Google.
 *
 * Si email.habilitado=false no intenta conectarse (útil en desarrollo).
 */
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final Properties CONFIG = new Properties();

    static {
        try (InputStream in = EmailService.class.getResourceAsStream("/config.properties")) {
            if (in != null) {
                CONFIG.load(in);
            } else {
                log.warn("config.properties no encontrado en classpath — email deshabilitado");
            }
        } catch (IOException e) {
            log.error("Error cargando config.properties", e);
        }
    }

    /**
     * Envía el correo de confirmación al cliente.
     *
     * @param destinatario  Email del cliente (puede ser null si no tiene)
     * @param nombreCliente Nombre completo del cliente
     * @param idReserva     ID de la reserva recién creada
     * @param numeroMesa    Ej: "M1"
     * @param fecha         Fecha de la reserva
     * @param hora          Hora de la reserva
     * @param comensales    Número de personas
     * @param importe       Importe de preautorización (ej: "30.00")
     * @return true si el email se envió correctamente, false en cualquier otro caso
     */
    public static boolean enviarConfirmacion(String destinatario,
                                              String nombreCliente,
                                              int    idReserva,
                                              String numeroMesa,
                                              LocalDate fecha,
                                              LocalTime hora,
                                              int    comensales,
                                              String importe) {

        if (!"true".equalsIgnoreCase(CONFIG.getProperty("email.habilitado", "false"))) {
            log.debug("Envío de email deshabilitado (email.habilitado=false)");
            return false;
        }
        if (destinatario == null || destinatario.isBlank()) {
            log.debug("Cliente sin email — no se envía confirmación");
            return false;
        }

        String host      = CONFIG.getProperty("email.smtp.host",     "smtp.gmail.com");
        String port      = CONFIG.getProperty("email.smtp.port",     "587");
        String usuario   = CONFIG.getProperty("email.smtp.usuario",  "");
        String remNombre = CONFIG.getProperty("email.remitente.nombre", "NEXERVO Restaurante");

        // La clave se lee primero de variable de entorno para no tenerla en el repo.
        // Si no hay var de entorno, se usa config.properties como fallback (desarrollo local).
        String clave = System.getenv("NEXERVO_EMAIL_CLAVE");
        if (clave == null || clave.isBlank()) {
            clave = CONFIG.getProperty("email.smtp.clave", "");
        }

        Properties smtpProps = new Properties();
        smtpProps.put("mail.smtp.auth",            "true");
        smtpProps.put("mail.smtp.starttls.enable", "true");
        smtpProps.put("mail.smtp.host",            host);
        smtpProps.put("mail.smtp.port",            port);
        smtpProps.put("mail.smtp.connectiontimeout", "8000");
        smtpProps.put("mail.smtp.timeout",           "8000");

        String finalClave = clave;
        Session session = Session.getInstance(smtpProps, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(usuario, finalClave);
            }
        });

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(usuario, remNombre, "UTF-8"));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destinatario));
            msg.setSubject("Reserva confirmada en NEXERVO · #" + idReserva);

            msg.setContent(construirHtml(nombreCliente, idReserva, numeroMesa,
                                         fecha, hora, comensales, importe),
                           "text/html; charset=UTF-8");
            Transport.send(msg);
            log.info("Confirmación de reserva #{} enviada a {}", idReserva, destinatario);
            return true;

        } catch (Exception e) {
            log.error("Error al enviar email de confirmación a {}", destinatario, e);
            return false;
        }
    }

    // ── Construcción del HTML del email ──────────────────────────────

    private static String construirHtml(String nombreCliente, int idReserva,
                                         String mesa, LocalDate fecha, LocalTime hora,
                                         int comensales, String importe) {

        String fechaStr     = fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String horaStr      = hora.format(DateTimeFormatter.ofPattern("HH:mm"));
        String nombreCorto  = nombreCliente.split(" ")[0];

        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
            </head>
            <body style="margin:0;padding:0;background-color:#0F0D1A;
                         font-family:'Helvetica Neue',Arial,sans-serif;">

              <table width="100%%" cellpadding="0" cellspacing="0"
                     style="background:#0F0D1A;padding:30px 0;">
                <tr><td align="center">

                  <!-- Contenedor principal -->
                  <table width="560" cellpadding="0" cellspacing="0"
                         style="background:#1A0A3C;border-radius:12px;
                                border:1px solid #3D2E8A;max-width:560px;">

                    <!-- CABECERA -->
                    <tr>
                      <td style="background:#0A0520;padding:24px 32px;
                                 border-bottom:1px solid #3D2E8A;border-radius:12px 12px 0 0;">
                        <p style="margin:0;font-size:26px;font-weight:bold;
                                  color:#00C6FF;letter-spacing:5px;font-family:Arial,sans-serif;">NEXERVO</p>
                        <p style="margin:6px 0 0;font-size:10px;color:#5050AA;
                                  letter-spacing:2px;font-family:Arial,sans-serif;">SISTEMA DE GESTIÓN DE RESERVAS</p>
                      </td>
                    </tr>

                    <!-- ICONO DE CONFIRMACIÓN -->
                    <tr>
                      <td align="center" style="padding:36px 32px 8px;">
                        <div style="width:68px;height:68px;border-radius:50%%;
                                    background:rgba(0,230,118,0.12);
                                    border:2px solid #00E676;
                                    font-size:18px;font-weight:bold;color:#00E676;
                                    line-height:68px;text-align:center;
                                    display:inline-block;font-family:Arial,sans-serif;">OK</div>
                      </td>
                    </tr>

                    <!-- TÍTULO -->
                    <tr>
                      <td align="center" style="padding:8px 32px 28px;">
                        <p style="margin:0;font-size:22px;font-weight:bold;color:#00E676;">
                          ¡Reserva confirmada!
                        </p>
                        <p style="margin:10px 0 0;font-size:14px;color:#9090BB;">
                          Hola, <strong style="color:#E8E8F0;">%s</strong>.
                          Tu reserva está lista.
                        </p>
                      </td>
                    </tr>

                    <!-- TABLA DE DETALLES -->
                    <tr>
                      <td style="padding:0 32px 28px;">
                        <table width="100%%" cellpadding="0" cellspacing="0"
                               style="background:rgba(10,5,35,0.7);border-radius:10px;
                                      border:1px solid #3D2E8A;">
                          <tr>
                            <td style="padding:13px 18px;color:#7070AA;font-size:11px;
                                       font-weight:bold;border-bottom:1px solid #2A1A6E;
                                       width:38%%;">Nº DE RESERVA</td>
                            <td style="padding:13px 18px;color:#00C6FF;font-size:14px;
                                       font-weight:bold;border-bottom:1px solid #2A1A6E;">
                              #%d
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:13px 18px;color:#7070AA;font-size:11px;
                                       font-weight:bold;border-bottom:1px solid #2A1A6E;">
                              MESA</td>
                            <td style="padding:13px 18px;color:#E8E8F0;font-size:14px;
                                       border-bottom:1px solid #2A1A6E;">%s</td>
                          </tr>
                          <tr>
                            <td style="padding:13px 18px;color:#7070AA;font-size:11px;
                                       font-weight:bold;border-bottom:1px solid #2A1A6E;">
                              FECHA Y HORA</td>
                            <td style="padding:13px 18px;color:#E8E8F0;font-size:14px;
                                       border-bottom:1px solid #2A1A6E;">
                              %s a las %s</td>
                          </tr>
                          <tr>
                            <td style="padding:13px 18px;color:#7070AA;font-size:11px;
                                       font-weight:bold;border-bottom:1px solid #2A1A6E;">
                              PERSONAS</td>
                            <td style="padding:13px 18px;color:#E8E8F0;font-size:14px;
                                       border-bottom:1px solid #2A1A6E;">
                              %d persona(s)</td>
                          </tr>
                          <tr>
                            <td style="padding:13px 18px;color:#7070AA;font-size:11px;
                                       font-weight:bold;">PREAUTORIZACIÓN</td>
                            <td style="padding:13px 18px;color:#00E676;font-size:16px;
                                       font-weight:bold;">%s €</td>
                          </tr>
                        </table>
                      </td>
                    </tr>

                    <!-- AVISO CANCELACIÓN -->
                    <tr>
                      <td style="padding:0 32px 28px;">
                        <table width="100%%" cellpadding="0" cellspacing="0"
                               style="background:rgba(255,193,7,0.07);border-radius:8px;
                                      border:1px solid rgba(255,193,7,0.35);">
                          <tr>
                            <td style="padding:13px 16px;font-size:12px;
                                       color:#FFD54F;line-height:1.6;">
                              Aviso: Si necesitas cancelar, hazlo con al menos
                              <strong>24 horas de antelación</strong> para que se devuelva
                              la preautorización. Puedes gestionar tu reserva desde la
                              sección <em>Mis Reservas</em> en la aplicación.
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>

                    <!-- PIE -->
                    <tr>
                      <td style="background:#0A0520;padding:18px 32px;
                                 border-top:1px solid #3D2E8A;text-align:center;
                                 border-radius:0 0 12px 12px;">
                        <p style="margin:0;font-size:11px;color:#3A3A5A;">
                          SIMULACIÓN · Proyecto académico TFC-DAM — no se realizan cargos reales
                        </p>
                        <p style="margin:5px 0 0;font-size:11px;color:#3A3A5A;">
                          NEXERVO · Sistema de Gestión de Reservas para Restaurante
                        </p>
                      </td>
                    </tr>

                  </table>
                </td></tr>
              </table>
            </body>
            </html>
            """.formatted(nombreCorto, idReserva, mesa, fechaStr, horaStr, comensales, importe);
    }
}
