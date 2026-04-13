package com.nexervo.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.nexervo.datos.ClienteDAO;
import com.nexervo.datos.EstadisticasDAO;
import com.nexervo.datos.MesaDAO;
import com.nexervo.datos.ReservaDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Servidor HTTP embebido para acceder a NEXERVO desde tablet o móvil.
 *
 * La idea surgió del feedback de mi tutor cuando le comenté que en hostelería
 * nadie va a sentarse delante de un PC de mesa para consultar las reservas del turno —
 * en sala siempre tienes una tablet o el móvil encima. Así que añadí este servidor
 * que arranca en paralelo a JavaFX y permite abrir el panel desde cualquier
 * dispositivo conectado a la red del local.
 *
 * Usé com.sun.net.httpserver porque es parte del JDK y no añade dependencias.
 * Al principio intenté con Javalin pero tiraba muchas dependencias transitivas
 * que complicaban el fat-jar con el shade plugin, así que preferí la opción más simple.
 *
 * Para JSON usé Gson — tuve que añadir adaptadores para LocalDate y LocalTime
 * porque Gson no los serializa por defecto y los campos de Reserva los usan.
 */
public class ApiServer {

    private static final Logger log = LoggerFactory.getLogger(ApiServer.class);
    public static final int PUERTO = 7070;

    private final HttpServer server;
    private final Gson gson;

    private final ReservaDAO    reservaDAO    = new ReservaDAO();
    private final ClienteDAO    clienteDAO    = new ClienteDAO();
    private final MesaDAO       mesaDAO       = new MesaDAO();
    private final EstadisticasDAO estadDAO    = new EstadisticasDAO();

    public ApiServer() throws IOException {
        // Gson necesita adaptadores para java.time — sin esto lanza IllegalArgumentException
        // y tardé un rato en entender por qué fallaba al serializar la lista de reservas
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new TypeAdapter<LocalDate>() {
                    @Override
                    public void write(JsonWriter out, LocalDate v) throws IOException {
                        out.value(v != null ? v.toString() : null);
                    }
                    @Override
                    public LocalDate read(JsonReader in) throws IOException {
                        return LocalDate.parse(in.nextString());
                    }
                })
                .registerTypeAdapter(LocalTime.class, new TypeAdapter<LocalTime>() {
                    @Override
                    public void write(JsonWriter out, LocalTime v) throws IOException {
                        out.value(v != null ? v.toString() : null);
                    }
                    @Override
                    public LocalTime read(JsonReader in) throws IOException {
                        return LocalTime.parse(in.nextString());
                    }
                })
                .setPrettyPrinting()
                .create();

        this.server = HttpServer.create(new InetSocketAddress(PUERTO), 0);

        // 4 hilos es más que suficiente para el volumen de un restaurante
        server.setExecutor(Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "nexervo-api-worker");
            t.setDaemon(true);
            return t;
        }));

        registrarContextos();
    }

    private void registrarContextos() {

        // Raíz → sirve la interfaz web
        server.createContext("/", this::servirInterfazWeb);

        // Reservas: GET por fecha / PATCH cambio de estado
        server.createContext("/api/reservas", ex -> {
            agregarCabeceras(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) {
                ex.sendResponseHeaders(204, -1);
                return;
            }
            String ruta = ex.getRequestURI().getPath();
            if (ruta.matches("/api/reservas/\\d+/estado")) {
                int id     = extraerIdRuta(ruta, "/api/reservas/", "/estado");
                String est = extraerParam(ex, "estado");
                // Si no viene el parámetro estado, devolvemos 400 en lugar de dejar
                // que el DAO lance un NullPointerException o actualice con null
                if (est == null || est.isBlank()) {
                    enviarJson(ex, 400, Map.of("ok", false, "error", "Falta el parámetro estado"));
                    return;
                }
                boolean ok = reservaDAO.cambiarEstado(id, est);
                enviarJson(ex, ok ? 200 : 400, Map.of("ok", ok, "idReserva", id));
            } else {
                String fechaStr = extraerParam(ex, "fecha");
                LocalDate fecha = (fechaStr != null) ? LocalDate.parse(fechaStr) : LocalDate.now();
                enviarJson(ex, 200, reservaDAO.obtenerPorFecha(fecha));
            }
        });

        server.createContext("/api/clientes", ex -> {
            agregarCabeceras(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
            enviarJson(ex, 200, clienteDAO.listarClientes());
        });

        server.createContext("/api/mesas", ex -> {
            agregarCabeceras(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
            enviarJson(ex, 200, mesaDAO.listarMesas());
        });

        // Dashboard — agrupa todos los KPIs en una sola llamada para no hacer
        // múltiples fetch() desde la interfaz web
        server.createContext("/api/dashboard", ex -> {
            agregarCabeceras(ex);
            if ("OPTIONS".equals(ex.getRequestMethod())) { ex.sendResponseHeaders(204, -1); return; }
            Map<String, Object> kpis = new LinkedHashMap<>();
            kpis.put("reservasHoy",    estadDAO.reservasHoy());
            kpis.put("totalClientes",  estadDAO.totalClientes());
            kpis.put("reservasMes",    estadDAO.reservasMes());
            kpis.put("ausenciasMes",   estadDAO.ausenciasMes());
            kpis.put("porDiaSemana",   estadDAO.reservasPorDiaSemana());
            kpis.put("porFranja",      estadDAO.reservasPorFranja());
            kpis.put("top5Clientes",   estadDAO.top5Clientes());
            kpis.put("ingresosPorMes", estadDAO.ingresosPorMes());
            enviarJson(ex, 200, kpis);
        });
    }

    public void iniciar() {
        Thread hilo = new Thread(() -> {
            server.start();
            log.info("Panel web disponible en http://localhost:{}/", PUERTO);
            log.info("API REST disponible en http://localhost:{}/api/", PUERTO);
        }, "nexervo-api-start");
        hilo.setDaemon(true);
        hilo.start();
    }

    public void detener() {
        if (server != null) {
            server.stop(1);
            log.info("API REST detenida.");
        }
    }

    private void servirInterfazWeb(HttpExchange ex) throws IOException {
        if (!"/".equals(ex.getRequestURI().getPath())) {
            responderError(ex, 404, "No encontrado.");
            return;
        }
        InputStream is = getClass().getResourceAsStream("/web/index.html");
        if (is == null) {
            responderError(ex, 500, "index.html no encontrado en el classpath.");
            return;
        }
        byte[] body = is.readAllBytes();
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        ex.sendResponseHeaders(200, body.length);
        ex.getResponseBody().write(body);
        ex.getResponseBody().close();
    }

    private void enviarJson(HttpExchange ex, int status, Object datos) throws IOException {
        byte[] body = gson.toJson(datos).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        ex.sendResponseHeaders(status, body.length);
        ex.getResponseBody().write(body);
        ex.getResponseBody().close();
    }

    private void responderError(HttpExchange ex, int status, String msg) throws IOException {
        byte[] body = msg.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, body.length);
        ex.getResponseBody().write(body);
        ex.getResponseBody().close();
    }

    // El navegador manda una petición OPTIONS antes del PATCH (CORS preflight)
    // — si no la respondemos, el navegador bloquea la llamada
    private void agregarCabeceras(HttpExchange ex) {
        ex.getResponseHeaders().set("Access-Control-Allow-Origin",  "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, PATCH, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private String extraerParam(HttpExchange ex, String clave) {
        String query = ex.getRequestURI().getQuery();
        if (query == null) return null;
        for (String par : query.split("&")) {
            String[] kv = par.split("=", 2);
            if (kv.length == 2 && kv[0].equals(clave)) return kv[1];
        }
        return null;
    }

    // Extrae el número de una ruta como /api/reservas/42/estado
    private int extraerIdRuta(String ruta, String prefijo, String sufijo) {
        String parte = ruta.substring(prefijo.length(), ruta.length() - sufijo.length());
        return Integer.parseInt(parte);
    }
}
