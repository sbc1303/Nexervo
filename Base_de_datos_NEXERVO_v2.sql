-- ============================================================
-- NEXERVO - Script de base de datos v2
-- Ejecutar completo: crea la BD desde cero y la puebla.
-- Compatible con MySQL 8 + InnoDB (propiedades ACID garantizadas).
-- ============================================================

DROP DATABASE IF EXISTS nexervo_db;
CREATE DATABASE nexervo_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE nexervo_db;

-- ============================================================
-- TABLA: usuarios
-- Gestiona el acceso al sistema por rol.
-- Se crea primero porque reservas la referenciará más adelante.
-- ============================================================
CREATE TABLE usuarios (
    id_usuario   INT          AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    usuario      VARCHAR(50)  NOT NULL UNIQUE,   -- login
    contrasena   VARCHAR(255) NOT NULL,           -- en producción: hash bcrypt
    rol          ENUM('ADMIN', 'EMPLEADO') NOT NULL DEFAULT 'EMPLEADO',
    activo       TINYINT(1)   NOT NULL DEFAULT 1,
    fecha_alta   DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- Datos de prueba: un admin y un empleado
-- Contraseñas en texto plano solo para desarrollo local.
-- En producción sustituir por hash bcrypt.
INSERT INTO usuarios (nombre, usuario, contrasena, rol) VALUES
('Administrador NEXERVO', 'admin',    'admin1234',    'ADMIN'),
('Empleado Sala',          'empleado', 'empleado1234', 'EMPLEADO');


-- ============================================================
-- TABLA: clientes
-- Centraliza la información de los clientes del restaurante.
-- El campo 'observaciones' es de uso libre para el personal.
-- YA NO almacena datos de reserva (eso va en la tabla reservas).
-- ============================================================
CREATE TABLE clientes (
    id_cliente      INT          AUTO_INCREMENT PRIMARY KEY,
    nombre_completo VARCHAR(100) NOT NULL,
    telefono        VARCHAR(15)  NOT NULL,
    email           VARCHAR(100),
    observaciones   TEXT,                          -- notas libres del personal
    fecha_registro  DATETIME     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

INSERT INTO clientes (nombre_completo, telefono, email, observaciones) VALUES
('Ana García',   '600111222', 'ana@email.com',    'Cliente VIP, prefiere mesa de ventana'),
('Carlos Ruiz',  '666777888', 'carlos@test.com',  'Llama siempre con poca antelación'),
('María López',  '611222333', 'maria@email.com',  NULL),
('Pedro Sánchez','622333444', 'pedro@email.com',  'Cumpleaños en marzo');


-- ============================================================
-- TABLA: mesas
-- Inventario físico del local.
-- numero_mesa: identificador visual (coincide con botones FXML M1-M12).
-- capacidad:   máximo de comensales sentados.
-- estado:      estado actual en tiempo real (lo gestiona el controlador).
-- ============================================================
CREATE TABLE mesas (
    id_mesa      INT         AUTO_INCREMENT PRIMARY KEY,
    numero_mesa  VARCHAR(10) NOT NULL UNIQUE,
    capacidad    INT         NOT NULL,
    estado       ENUM('LIBRE', 'OCUPADA', 'RESERVADA') DEFAULT 'LIBRE',
    descripcion  VARCHAR(255)
) ENGINE=InnoDB;

-- 12 mesas para coincidir con los 12 botones del FXML (M1-M12)
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
-- Núcleo relacional del sistema.
-- Vincula cliente + mesa + fecha/hora.
-- estado_reserva: ciclo de vida completo de la reserva.
-- ============================================================
CREATE TABLE reservas (
    id_reserva     INT      AUTO_INCREMENT PRIMARY KEY,
    id_cliente     INT      NOT NULL,
    id_mesa        INT      NOT NULL,
    fecha_reserva  DATE     NOT NULL,
    hora_reserva   TIME     NOT NULL,
    comensales     INT      NOT NULL DEFAULT 2,
    estado_reserva ENUM('CONFIRMADA', 'CANCELADA', 'FINALIZADA') NOT NULL DEFAULT 'CONFIRMADA',
    observaciones  TEXT,                           -- notas específicas de esta reserva
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reserva_cliente
        FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente)
        ON DELETE CASCADE,                         -- si se borra el cliente, se borran sus reservas

    CONSTRAINT fk_reserva_mesa
        FOREIGN KEY (id_mesa)    REFERENCES mesas(id_mesa)
        ON DELETE RESTRICT                         -- no se puede borrar una mesa con reservas
) ENGINE=InnoDB;

-- Reservas de prueba (usan id_cliente e id_mesa reales de arriba)
INSERT INTO reservas (id_cliente, id_mesa, fecha_reserva, hora_reserva, comensales, estado_reserva) VALUES
(1, 1, '2026-04-01', '14:00:00', 2, 'CONFIRMADA'),
(2, 3, '2026-04-01', '21:00:00', 4, 'CONFIRMADA'),
(3, 7, '2026-04-02', '13:30:00', 5, 'CONFIRMADA');


-- ============================================================
-- TABLA: intolerancias
-- Catálogo de alérgenos e intolerancias.
-- Los primeros 14 son los alérgenos de declaración obligatoria
-- según el Reglamento (UE) 1169/2011.
-- A partir del 15 se añaden intolerancias comunes no reguladas.
-- El controlador agrupa en dos secciones usando subList(0,14)
-- y subList(14, size()), así que el orden aquí es importante.
-- ============================================================
CREATE TABLE intolerancias (
    id_intolerancia INT         AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    tipo            ENUM('ALERGENO_UE', 'INTOLERANCIA') NOT NULL DEFAULT 'ALERGENO_UE'
) ENGINE=InnoDB;

-- 14 alérgenos oficiales UE (Reglamento 1169/2011) — posiciones 1-14
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

-- Intolerancias comunes no reguladas — posiciones 15 en adelante
('Fructosa',                                      'INTOLERANCIA'),
('Sorbitol',                                      'INTOLERANCIA'),
('Histamina',                                     'INTOLERANCIA'),
('FODMAP (síndrome intestino irritable)',          'INTOLERANCIA'),
('Salicilatos',                                   'INTOLERANCIA');


-- ============================================================
-- TABLA: clientes_intolerancias
-- Relación N:M entre clientes e intolerancias.
-- Permite registrar múltiples alérgenos por cliente.
-- ============================================================
CREATE TABLE clientes_intolerancias (
    id_cliente      INT NOT NULL,
    id_intolerancia INT NOT NULL,
    PRIMARY KEY (id_cliente, id_intolerancia),
    FOREIGN KEY (id_cliente)      REFERENCES clientes(id_cliente)      ON DELETE CASCADE,
    FOREIGN KEY (id_intolerancia) REFERENCES intolerancias(id_intolerancia) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Ana García: gluten + frutos de cáscara
INSERT INTO clientes_intolerancias VALUES (1, 1), (1, 8);
-- Carlos Ruiz: marisco (crustáceos + moluscos)
INSERT INTO clientes_intolerancias VALUES (2, 2), (2, 14);


-- ============================================================
-- TABLA: preautorizaciones_pago
-- Simula el ciclo de vida de un cobro preventivo por reserva.
-- No se integra con pasarelas reales (ver memoria, apartado 2.3.2).
-- estado: pendiente → autorizada → devuelta (servicio OK)
--                   → rechazada   (pago denegado)
--                   → devuelta    (no-show o cancelación)
-- ============================================================
CREATE TABLE preautorizaciones_pago (
    id_pago        INT            AUTO_INCREMENT PRIMARY KEY,
    id_reserva     INT            NOT NULL UNIQUE,  -- 1 preautorización por reserva
    importe        DECIMAL(8, 2)  NOT NULL,
    estado         ENUM('PENDIENTE', 'AUTORIZADA', 'RECHAZADA', 'DEVUELTA')
                   NOT NULL DEFAULT 'PENDIENTE',
    fecha_creacion DATETIME       DEFAULT CURRENT_TIMESTAMP,
    fecha_gestion  DATETIME,                        -- cuando cambia de estado

    CONSTRAINT fk_pago_reserva
        FOREIGN KEY (id_reserva) REFERENCES reservas(id_reserva)
        ON DELETE CASCADE
) ENGINE=InnoDB;

-- Preautorización de prueba para la reserva 1 (Ana García)
INSERT INTO preautorizaciones_pago (id_reserva, importe, estado) VALUES
(1, 20.00, 'AUTORIZADA');


-- ============================================================
-- VERIFICACIÓN FINAL
-- Muestra el recuento de registros en cada tabla.
-- ============================================================
SELECT 'usuarios'                AS tabla, COUNT(*) AS registros FROM usuarios
UNION ALL
SELECT 'clientes',                          COUNT(*) FROM clientes
UNION ALL
SELECT 'mesas',                             COUNT(*) FROM mesas
UNION ALL
SELECT 'reservas',                          COUNT(*) FROM reservas
UNION ALL
SELECT 'intolerancias',                     COUNT(*) FROM intolerancias
UNION ALL
SELECT 'clientes_intolerancias',            COUNT(*) FROM clientes_intolerancias
UNION ALL
SELECT 'preautorizaciones_pago',            COUNT(*) FROM preautorizaciones_pago;
