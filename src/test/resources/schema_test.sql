-- ============================================================
-- NEXERVO — Schema para tests de integración (H2 en memoria)
-- Compatible con H2 en modo MySQL (MODE=MySQL).
-- Se ejecuta antes de cada clase de test para tener BD limpia.
-- ============================================================

CREATE TABLE IF NOT EXISTS usuarios (
    id_usuario   INT          AUTO_INCREMENT PRIMARY KEY,
    nombre       VARCHAR(100) NOT NULL,
    usuario      VARCHAR(50)  NOT NULL UNIQUE,
    contrasena   VARCHAR(255) NOT NULL,
    rol          VARCHAR(20)  NOT NULL DEFAULT 'EMPLEADO',
    activo       TINYINT(1)   NOT NULL DEFAULT 1,
    fecha_alta   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS clientes (
    id_cliente      INT          AUTO_INCREMENT PRIMARY KEY,
    nombre_completo VARCHAR(100) NOT NULL,
    telefono        VARCHAR(15)  NOT NULL,
    email           VARCHAR(100),
    observaciones   TEXT,
    fecha_registro  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS mesas (
    id_mesa      INT         AUTO_INCREMENT PRIMARY KEY,
    numero_mesa  VARCHAR(10) NOT NULL UNIQUE,
    capacidad    INT         NOT NULL,
    estado       VARCHAR(20) DEFAULT 'LIBRE',
    descripcion  VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS reservas (
    id_reserva            INT      AUTO_INCREMENT PRIMARY KEY,
    id_cliente            INT      NOT NULL,
    id_mesa               INT      NOT NULL,
    fecha_reserva         DATE     NOT NULL,
    hora_reserva          TIME     NOT NULL,
    comensales            INT      NOT NULL DEFAULT 2,
    estado_reserva        VARCHAR(20) NOT NULL DEFAULT 'CONFIRMADA',
    ocasion               VARCHAR(50) DEFAULT 'Ninguna',
    peticiones_especiales TEXT,
    observaciones         TEXT,
    fecha_creacion        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente) ON DELETE CASCADE,
    FOREIGN KEY (id_mesa)    REFERENCES mesas(id_mesa)       ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS intolerancias (
    id_intolerancia INT          AUTO_INCREMENT PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    tipo            VARCHAR(20)  NOT NULL DEFAULT 'ALERGENO_UE'
);

CREATE TABLE IF NOT EXISTS clientes_intolerancias (
    id_cliente      INT NOT NULL,
    id_intolerancia INT NOT NULL,
    PRIMARY KEY (id_cliente, id_intolerancia),
    FOREIGN KEY (id_cliente)      REFERENCES clientes(id_cliente)          ON DELETE CASCADE,
    FOREIGN KEY (id_intolerancia) REFERENCES intolerancias(id_intolerancia) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS preautorizaciones_pago (
    id_pago        INT            AUTO_INCREMENT PRIMARY KEY,
    id_reserva     INT            NOT NULL UNIQUE,
    importe        DECIMAL(8, 2)  NOT NULL,
    estado         VARCHAR(20)    NOT NULL DEFAULT 'PENDIENTE',
    fecha_creacion TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    fecha_gestion  TIMESTAMP,
    FOREIGN KEY (id_reserva) REFERENCES reservas(id_reserva) ON DELETE CASCADE
);

-- Datos base para los tests
INSERT INTO mesas (numero_mesa, capacidad, descripcion) VALUES
('M1', 2,  'Mesa junto a ventana'),
('M2', 4,  'Mesa central'),
('M3', 6,  'Mesa grande');

INSERT INTO intolerancias (nombre, tipo) VALUES
('Gluten (trigo, centeno, cebada, avena)', 'ALERGENO_UE'),
('Leche y derivados (lactosa)',            'ALERGENO_UE'),
('Huevos',                                 'ALERGENO_UE');
