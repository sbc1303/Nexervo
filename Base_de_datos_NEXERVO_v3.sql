-- ============================================================
-- NEXERVO - Script de base de datos v3
-- Cambios respecto a v2:
--   · estado_reserva incluye 'No presentado' (no se presentó)
--   · columna ocasion VARCHAR(50) en reservas
--   · peticiones_especiales TEXT en reservas
--   · num_visitas view (historial cliente)
-- Compatible con MySQL 8 + InnoDB (propiedades ACID garantizadas).
-- Basado en Reglamento (UE) 1169/2011 — 14 alérgenos obligatorios.
-- ============================================================

DROP DATABASE IF EXISTS nexervo_db;
CREATE DATABASE nexervo_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nexervo_db;

-- ============================================================
-- TABLA: usuarios
-- ============================================================
CREATE TABLE usuarios (
    id_usuario   INT          AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    usuario      VARCHAR(50)  NOT NULL UNIQUE,
    contrasena   VARCHAR(255) NOT NULL,            -- en producción: hash bcrypt
    rol          ENUM('ADMIN', 'EMPLEADO') NOT NULL DEFAULT 'EMPLEADO',
    activo       TINYINT(1)   NOT NULL DEFAULT 1,
    fecha_alta   DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

INSERT INTO usuarios (nombre, usuario, contrasena, rol) VALUES
('Administrador NEXERVO', 'admin',    'admin1234',    'ADMIN'),
('Empleado Sala',          'empleado', 'empleado1234', 'EMPLEADO');


-- ============================================================
-- TABLA: clientes
-- ============================================================
CREATE TABLE clientes (
    id_cliente      INT          AUTO_INCREMENT PRIMARY KEY,
    nombre_completo VARCHAR(100) NOT NULL,
    telefono        VARCHAR(15)  NOT NULL,
    email           VARCHAR(100),
    observaciones   TEXT,
    fecha_registro  DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

INSERT INTO clientes (nombre_completo, telefono, email, observaciones) VALUES
('Ana García',    '600111222', 'ana@email.com',    'Cliente VIP, prefiere mesa de ventana'),
('Carlos Ruiz',   '666777888', 'carlos@test.com',  'Llama siempre con poca antelación'),
('María López',   '611222333', 'maria@email.com',  NULL),
('Pedro Sánchez', '622333444', 'pedro@email.com',  'Cumpleaños en marzo'),
('Laura Martín',  '633444555', 'laura@email.com',  'Habitual los viernes');


-- ============================================================
-- TABLA: mesas
-- ============================================================
CREATE TABLE mesas (
    id_mesa      INT         AUTO_INCREMENT PRIMARY KEY,
    numero_mesa  VARCHAR(10) NOT NULL UNIQUE,
    capacidad    INT         NOT NULL,
    estado       ENUM('LIBRE', 'OCUPADA', 'RESERVADA') DEFAULT 'LIBRE',
    descripcion  VARCHAR(255)
) ENGINE=InnoDB;

INSERT INTO mesas (numero_mesa, capacidad, descripcion) VALUES
('M1',  2,  'Mesa junto a ventana, zona tranquila'),
('M2',  2,  'Mesa junto a ventana, zona tranquila'),
('M3',  4,  'Mesa central, buena para familias'),
('M4',  4,  'Mesa central'),
('M5',  4,  'Mesa central'),
('M6',  4,  'Mesa zona bar'),
('M7',  6,  'Mesa grande, ideal para grupos'),
('M8',  6,  'Mesa grande, zona privada'),
('M9',  2,  'Mesa terraza interior'),
('M10', 2,  'Mesa terraza interior'),
('M11', 8,  'Mesa larga para eventos o grupos grandes'),
('M12', 4,  'Mesa junto a la entrada');


-- ============================================================
-- TABLA: reservas
-- ============================================================
-- CAMBIOS v3:
--   · añadí NO_PRESENTADO al ENUM de estado_reserva
--     (me di cuenta de que CANCELADA no cubre el caso de que el cliente
--      no aparezca — son situaciones muy distintas en la gestión del local)
--   · columna 'ocasion': para anotar si es cumpleaños, aniversario, etc.
--   · columna 'peticiones_especiales': silla de bebé, PMR, decoración...
-- ============================================================
CREATE TABLE reservas (
    id_reserva           INT      AUTO_INCREMENT PRIMARY KEY,
    id_cliente           INT      NOT NULL,
    id_mesa              INT      NOT NULL,
    fecha_reserva        DATE     NOT NULL,
    hora_reserva         TIME     NOT NULL,
    comensales           INT      NOT NULL DEFAULT 2,
    estado_reserva       ENUM('CONFIRMADA','CANCELADA','FINALIZADA','No presentado')
                         NOT NULL DEFAULT 'CONFIRMADA',
    ocasion              VARCHAR(50) DEFAULT 'Ninguna',
    peticiones_especiales TEXT,
    observaciones        TEXT,
    fecha_creacion       DATETIME DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reserva_cliente
        FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente) ON DELETE CASCADE,
    CONSTRAINT fk_reserva_mesa
        FOREIGN KEY (id_mesa)    REFERENCES mesas(id_mesa)       ON DELETE RESTRICT
) ENGINE=InnoDB;

INSERT INTO reservas (id_cliente, id_mesa, fecha_reserva, hora_reserva, comensales, estado_reserva, ocasion, peticiones_especiales) VALUES
(1, 1, '2026-04-01', '14:00:00', 2, 'CONFIRMADA',  'Aniversario',    'Decoración especial con flores'),
(2, 3, '2026-04-01', '21:00:00', 4, 'CONFIRMADA',  'Ninguna',        NULL),
(3, 7, '2026-04-02', '13:30:00', 5, 'CONFIRMADA',  'Ninguna',        'Silla de bebé necesaria'),
(4, 2, '2026-04-03', '21:00:00', 2, 'FINALIZADA',  'Cumpleaños',     'Tarta sorpresa'),
(5, 5, '2026-04-04', '14:00:00', 3, 'No presentado',     'Ninguna',        NULL),
(1, 4, '2026-03-15', '13:00:00', 2, 'FINALIZADA',  'Ninguna',        NULL),
(1, 8, '2026-02-14', '21:00:00', 2, 'FINALIZADA',  'Aniversario',    'Mesa más íntima posible'),
(2, 6, '2026-03-01', '14:30:00', 4, 'FINALIZADA',  'Reunión empresa','Proyector si es posible');


-- ============================================================
-- TABLA: intolerancias
-- Los 14 alérgenos son de declaración OBLIGATORIA según
-- el Reglamento (UE) 1169/2011 sobre información alimentaria.
-- ============================================================
CREATE TABLE intolerancias (
    id_intolerancia INT          AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    tipo            ENUM('ALERGENO_UE', 'INTOLERANCIA') NOT NULL DEFAULT 'ALERGENO_UE'
) ENGINE=InnoDB;

-- 14 alérgenos de declaración OBLIGATORIA — Reglamento (UE) 1169/2011
-- En hostelería es obligatorio informar de estos alérgenos en carta y verbalmente si se pide
INSERT INTO intolerancias (nombre, tipo) VALUES
('Gluten (trigo, centeno, cebada, avena)',       'ALERGENO_UE'),
('Crustáceos',                                    'ALERGENO_UE'),
('Huevos',                                        'ALERGENO_UE'),
('Pescado',                                       'ALERGENO_UE'),
('Cacahuetes',                                    'ALERGENO_UE'),
('Soja',                                          'ALERGENO_UE'),
('Leche y derivados (lactosa)',                   'ALERGENO_UE'),
('Frutos de cáscara (nueces, almendras, etc.)',   'ALERGENO_UE'),
('Apio',                                          'ALERGENO_UE'),
('Mostaza',                                       'ALERGENO_UE'),
('Granos de sésamo',                              'ALERGENO_UE'),
('Dióxido de azufre y sulfitos',                  'ALERGENO_UE'),
('Altramuces',                                    'ALERGENO_UE'),
('Moluscos',                                      'ALERGENO_UE'),
-- Intolerancias comunes no reguladas
('Fructosa',                                      'INTOLERANCIA'),
('Sorbitol',                                      'INTOLERANCIA'),
('Histamina',                                     'INTOLERANCIA'),
('FODMAP (síndrome intestino irritable)',          'INTOLERANCIA'),
('Salicilatos',                                   'INTOLERANCIA');


-- ============================================================
-- TABLA: clientes_intolerancias
-- ============================================================
CREATE TABLE clientes_intolerancias (
    id_cliente      INT NOT NULL,
    id_intolerancia INT NOT NULL,
    PRIMARY KEY (id_cliente, id_intolerancia),
    FOREIGN KEY (id_cliente)      REFERENCES clientes(id_cliente)          ON DELETE CASCADE,
    FOREIGN KEY (id_intolerancia) REFERENCES intolerancias(id_intolerancia) ON DELETE CASCADE
) ENGINE=InnoDB;

INSERT INTO clientes_intolerancias VALUES
(1, 1), (1, 8),    -- Ana: gluten + frutos de cáscara
(2, 2), (2, 14),   -- Carlos: crustáceos + moluscos
(3, 7);            -- María: lactosa


-- ============================================================
-- TABLA: preautorizaciones_pago
-- ============================================================
CREATE TABLE preautorizaciones_pago (
    id_pago        INT            AUTO_INCREMENT PRIMARY KEY,
    id_reserva     INT            NOT NULL UNIQUE,
    importe        DECIMAL(8, 2)  NOT NULL,
    estado         ENUM('PENDIENTE','AUTORIZADA','RECHAZADA','DEVUELTA')
                   NOT NULL DEFAULT 'PENDIENTE',
    fecha_creacion DATETIME       DEFAULT CURRENT_TIMESTAMP,
    fecha_gestion  DATETIME,
    CONSTRAINT fk_pago_reserva
        FOREIGN KEY (id_reserva) REFERENCES reservas(id_reserva) ON DELETE CASCADE
) ENGINE=InnoDB;

INSERT INTO preautorizaciones_pago (id_reserva, importe, estado) VALUES
(1, 20.00, 'AUTORIZADA'),
(4, 20.00, 'DEVUELTA'),
(6, 20.00, 'DEVUELTA'),
(7, 20.00, 'DEVUELTA');


-- ============================================================
-- VISTA: resumen de visitas por cliente
-- Útil para el panel de historial en la ficha del cliente.
-- ============================================================
CREATE VIEW vista_frecuencia_clientes AS
SELECT
    c.id_cliente,
    c.nombre_completo,
    COUNT(r.id_reserva)                                    AS total_reservas,
    SUM(CASE WHEN r.estado_reserva = 'FINALIZADA' THEN 1 ELSE 0 END) AS visitas_completadas,
    SUM(CASE WHEN r.estado_reserva = 'No presentado'    THEN 1 ELSE 0 END) AS ausencias,
    SUM(CASE WHEN r.estado_reserva = 'FINALIZADA' THEN r.comensales * 10.00 ELSE 0 END) AS gasto_estimado,
    MAX(CASE WHEN r.estado_reserva = 'FINALIZADA' THEN r.fecha_reserva END) AS ultima_visita
FROM clientes c
LEFT JOIN reservas r ON c.id_cliente = r.id_cliente
GROUP BY c.id_cliente, c.nombre_completo;


-- ============================================================
-- VERIFICACIÓN FINAL
-- ============================================================
SELECT 'usuarios'             AS tabla, COUNT(*) AS registros FROM usuarios
UNION ALL SELECT 'clientes',                  COUNT(*) FROM clientes
UNION ALL SELECT 'mesas',                     COUNT(*) FROM mesas
UNION ALL SELECT 'reservas',                  COUNT(*) FROM reservas
UNION ALL SELECT 'intolerancias',             COUNT(*) FROM intolerancias
UNION ALL SELECT 'clientes_intolerancias',    COUNT(*) FROM clientes_intolerancias
UNION ALL SELECT 'preautorizaciones_pago',    COUNT(*) FROM preautorizaciones_pago;
