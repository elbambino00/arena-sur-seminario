-- ==========================================
-- ARENA SUR
-- BASE DE DATOS DEL SISTEMA DE RESERVAS
-- ==========================================


-- ==========================================
-- 1. CREACIÓN DE BASE DE DATOS
-- ==========================================

CREATE DATABASE IF NOT EXISTS arena_sur
CHARACTER SET utf8mb4
COLLATE utf8mb4_0900_ai_ci

USE arena_sur;

-- ==========================================
-- 2. CREACIÓN DE TABLAS
-- ==========================================

CREATE TABLE deporte (
    id_deporte BIGINT AUTO_INCREMENT,
    nombre VARCHAR(50) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_deporte
        PRIMARY KEY (id_deporte),

    CONSTRAINT uq_deporte_nombre
        UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE cliente (
    id_cliente BIGINT AUTO_INCREMENT,
    nombre VARCHAR(60) NOT NULL,
    apellido VARCHAR(60) NOT NULL,
    telefono VARCHAR(30) NOT NULL,
    email VARCHAR(120) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_cliente
        PRIMARY KEY (id_cliente),

    CONSTRAINT uq_cliente_email
        UNIQUE (email)
) ENGINE = InnoDB;

CREATE TABLE usuario (
    id_usuario BIGINT AUTO_INCREMENT,
    nombre_usuario VARCHAR(60) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    rol ENUM(
        'ADMINISTRADOR',
        'RECEPCIONISTA'
    ) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_usuario
        PRIMARY KEY (id_usuario),

    CONSTRAINT uq_usuario_nombre
        UNIQUE (nombre_usuario)
) ENGINE = InnoDB;

CREATE TABLE cancha (
    id_cancha BIGINT AUTO_INCREMENT,
    nombre VARCHAR(60) NOT NULL,
    id_deporte BIGINT NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_cancha
        PRIMARY KEY (id_cancha),

    CONSTRAINT fk_cancha_deporte
        FOREIGN KEY (id_deporte)
        REFERENCES deporte(id_deporte)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE = InnoDB;

CREATE TABLE tarifa (
    id_tarifa BIGINT AUTO_INCREMENT,
    id_deporte BIGINT NOT NULL,
    duracion_minutos INT NOT NULL,
    precio DECIMAL(10,2) NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT pk_tarifa
        PRIMARY KEY (id_tarifa),

    CONSTRAINT fk_tarifa_deporte
        FOREIGN KEY (id_deporte)
        REFERENCES deporte(id_deporte)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT chk_tarifa_duracion
        CHECK (duracion_minutos IN (60, 90)),

    CONSTRAINT chk_tarifa_precio
        CHECK (precio > 0)
) ENGINE = InnoDB;

CREATE TABLE reserva (
    id_reserva BIGINT AUTO_INCREMENT,

    id_cliente BIGINT NOT NULL,
    id_cancha BIGINT NOT NULL,
    id_tarifa BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,

    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    duracion_minutos INT NOT NULL,

    estado ENUM(
        'PENDIENTE',
        'CONFIRMADA',
        'CANCELADA'
    ) NOT NULL DEFAULT 'PENDIENTE',

    precio_total DECIMAL(10,2) NOT NULL,
    importe_sena DECIMAL(10,2) NOT NULL,
    credito_favor DECIMAL(10,2) NOT NULL DEFAULT 0.00,

    fecha_hora_creacion DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vencimiento_sena DATETIME NOT NULL,

    CONSTRAINT pk_reserva
        PRIMARY KEY (id_reserva),

    CONSTRAINT fk_reserva_cliente
        FOREIGN KEY (id_cliente)
        REFERENCES cliente(id_cliente)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_reserva_cancha
        FOREIGN KEY (id_cancha)
        REFERENCES cancha(id_cancha)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_reserva_tarifa
        FOREIGN KEY (id_tarifa)
        REFERENCES tarifa(id_tarifa)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_reserva_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT chk_reserva_duracion
        CHECK (duracion_minutos IN (60, 90)),

    CONSTRAINT chk_reserva_precio
        CHECK (precio_total > 0),

    CONSTRAINT chk_reserva_sena
        CHECK (importe_sena >= 0),

    CONSTRAINT chk_reserva_credito
        CHECK (credito_favor >= 0)
) ENGINE = InnoDB;

CREATE TABLE pago (
    id_pago BIGINT AUTO_INCREMENT,
    id_reserva BIGINT NOT NULL,
    id_usuario BIGINT NOT NULL,

    tipo ENUM(
        'SENA',
        'SALDO',
        'DEVOLUCION'
    ) NOT NULL,

    importe DECIMAL(10,2) NOT NULL,
    fecha_hora DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_pago
        PRIMARY KEY (id_pago),

    CONSTRAINT fk_pago_reserva
        FOREIGN KEY (id_reserva)
        REFERENCES reserva(id_reserva)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT fk_pago_usuario
        FOREIGN KEY (id_usuario)
        REFERENCES usuario(id_usuario)
        ON UPDATE CASCADE
        ON DELETE RESTRICT,

    CONSTRAINT chk_pago_importe
        CHECK (importe > 0)
) ENGINE = InnoDB;

-- ==========================================
-- 3. ÍNDICES
-- ==========================================

CREATE INDEX idx_reserva_cancha_fecha
ON reserva (id_cancha, fecha, estado, hora_inicio);

CREATE INDEX idx_reserva_cliente
ON reserva (id_cliente);

CREATE INDEX idx_pago_reserva
ON pago (id_reserva);

CREATE INDEX idx_tarifa_deporte_duracion
ON tarifa (id_deporte, duracion_minutos, activa);

-- ==========================================
-- 4. DATOS DE PRUEBA
-- ==========================================

INSERT INTO deporte (nombre, activo)
VALUES
    ('Fútbol 5', TRUE),
    ('Pádel', TRUE),
    ('Tenis', TRUE);
    
INSERT INTO cancha (nombre, id_deporte, activa)
VALUES
    ('Fútbol 1',
        (SELECT id_deporte FROM deporte WHERE nombre = 'Fútbol 5'),
        TRUE),

    ('Fútbol 2',
        (SELECT id_deporte FROM deporte WHERE nombre = 'Fútbol 5'),
        TRUE),

    ('Pádel 1',
        (SELECT id_deporte FROM deporte WHERE nombre = 'Pádel'),
        TRUE),

    ('Pádel 2',
        (SELECT id_deporte FROM deporte WHERE nombre = 'Pádel'),
        TRUE),

    ('Tenis 1',
        (SELECT id_deporte FROM deporte WHERE nombre = 'Tenis'),
        TRUE);
        
	INSERT INTO tarifa (
    id_deporte,
    duracion_minutos,
    precio,
    activa
)
VALUES
(
    (SELECT id_deporte FROM deporte WHERE nombre = 'Fútbol 5'),
    60,
    60000.00,
    TRUE
),
(
    (SELECT id_deporte FROM deporte WHERE nombre = 'Fútbol 5'),
    90,
    90000.00,
    TRUE
),
(
    (SELECT id_deporte FROM deporte WHERE nombre = 'Pádel'),
    60,
    55000.00,
    TRUE
),
(
    (SELECT id_deporte FROM deporte WHERE nombre = 'Pádel'),
    90,
    80000.00,
    TRUE
),
(
    (SELECT id_deporte FROM deporte WHERE nombre = 'Tenis'),
    60,
    40000.00,
    TRUE
),
(
    (SELECT id_deporte FROM deporte WHERE nombre = 'Tenis'),
    90,
    60000.00,
    TRUE
);

INSERT INTO usuario (
    nombre_usuario,
    password_hash,
    rol,
    activo
)
VALUES
(
    'admin',
    'HASH_ADMIN_PRUEBA',
    'ADMINISTRADOR',
    TRUE
),
(
    'recepcion1',
    'HASH_RECEPCION_PRUEBA',
    'RECEPCIONISTA',
    TRUE
);

INSERT INTO cliente (
    nombre,
    apellido,
    telefono,
    email,
    activo
)
VALUES
(
    'Juan',
    'Pérez',
    '11-4567-1001',
    'juan.perez@example.com',
    TRUE
),
(
    'Lucía',
    'Gómez',
    '11-4567-1002',
    'lucia.gomez@example.com',
    TRUE
),
(
    'Martín',
    'López',
    '11-4567-1003',
    'martin.lopez@example.com',
    TRUE
);

INSERT INTO reserva (
    id_cliente,
    id_cancha,
    id_tarifa,
    id_usuario,
    fecha,
    hora_inicio,
    duracion_minutos,
    estado,
    precio_total,
    importe_sena,
    credito_favor,
    fecha_hora_creacion,
    vencimiento_sena
)
VALUES (
    (
        SELECT id_cliente
        FROM cliente
        WHERE email = 'juan.perez@example.com'
    ),

    (
        SELECT id_cancha
        FROM cancha
        WHERE nombre = 'Fútbol 1'
    ),

    (
        SELECT t.id_tarifa
        FROM tarifa t
        JOIN deporte d
            ON t.id_deporte = d.id_deporte
        WHERE d.nombre = 'Fútbol 5'
          AND t.duracion_minutos = 60
          AND t.activa = TRUE
        LIMIT 1
    ),

    (
        SELECT id_usuario
        FROM usuario
        WHERE nombre_usuario = 'recepcion1'
    ),

    '2026-10-10',
    '18:00:00',
    60,
    'PENDIENTE',
    60000.00,
    12000.00,
    0.00,
    '2026-10-01 10:00:00',
    '2026-10-01 10:30:00'
);

INSERT INTO reserva (
    id_cliente,
    id_cancha,
    id_tarifa,
    id_usuario,
    fecha,
    hora_inicio,
    duracion_minutos,
    estado,
    precio_total,
    importe_sena,
    credito_favor,
    fecha_hora_creacion,
    vencimiento_sena
)
VALUES (
    (
        SELECT id_cliente
        FROM cliente
        WHERE email = 'lucia.gomez@example.com'
    ),

    (
        SELECT id_cancha
        FROM cancha
        WHERE nombre = 'Pádel 1'
    ),

    (
        SELECT t.id_tarifa
        FROM tarifa t
        JOIN deporte d
            ON t.id_deporte = d.id_deporte
        WHERE d.nombre = 'Pádel'
          AND t.duracion_minutos = 90
          AND t.activa = TRUE
        LIMIT 1
    ),

    (
        SELECT id_usuario
        FROM usuario
        WHERE nombre_usuario = 'admin'
    ),

    '2026-10-11',
    '20:00:00',
    90,
    'CONFIRMADA',
    80000.00,
    16000.00,
    0.00,
    '2026-10-02 15:00:00',
    '2026-10-02 15:30:00'
);

INSERT INTO pago (
    id_reserva,
    id_usuario,
    tipo,
    importe,
    fecha_hora
)
VALUES (
    (
        SELECT r.id_reserva
        FROM reserva r
        JOIN cliente c
            ON r.id_cliente = c.id_cliente
        WHERE c.email = 'lucia.gomez@example.com'
          AND r.fecha = '2026-10-11'
          AND r.hora_inicio = '20:00:00'
        LIMIT 1
    ),

    (
        SELECT id_usuario
        FROM usuario
        WHERE nombre_usuario = 'admin'
    ),

    'SENA',
    16000.00,
    '2026-10-02 15:10:00'
);

-- ==========================================
-- 5. CONSULTAS
-- ==========================================

-- =========================================================
-- 5.1. CONSULTA DE AGENDA DIARIA
-- Muestra las reservas vigentes de una fecha determinada.
-- Relacionada con CU10 - Consultar agenda de canchas.
-- =========================================================

SET @fecha_agenda = '2026-10-10';

SELECT
    r.id_reserva,
    ca.nombre AS cancha,
    d.nombre AS deporte,
    r.hora_inicio,
    r.duracion_minutos,
    CONCAT(c.nombre, ' ', c.apellido) AS cliente,
    r.estado
FROM reserva r
JOIN cliente c
    ON r.id_cliente = c.id_cliente
JOIN cancha ca
    ON r.id_cancha = ca.id_cancha
JOIN deporte d
    ON ca.id_deporte = d.id_deporte
WHERE r.fecha = @fecha_agenda
  AND r.estado <> 'CANCELADA'
ORDER BY ca.nombre, r.hora_inicio;



-- =========================================================
-- 5.2. CONSULTA DE DISPONIBILIDAD DE CANCHAS
-- Busca canchas disponibles para un deporte, fecha,
-- horario y duración determinados.
-- Relacionada con CU03 - Consultar disponibilidad.
-- =========================================================

SET @deporte_solicitado = 'Fútbol 5';
SET @fecha_solicitada = '2026-10-10';
SET @hora_solicitada = '18:30:00';
SET @duracion_solicitada = 60;

SELECT
    ca.id_cancha,
    ca.nombre AS cancha,
    d.nombre AS deporte
FROM cancha ca
JOIN deporte d
    ON ca.id_deporte = d.id_deporte
WHERE d.nombre = @deporte_solicitado
  AND ca.activa = TRUE
  AND NOT EXISTS (
        SELECT 1
        FROM reserva r
        WHERE r.id_cancha = ca.id_cancha
          AND r.fecha = @fecha_solicitada
          AND r.estado IN ('PENDIENTE', 'CONFIRMADA')

          -- Existe superposición cuando el nuevo inicio
          -- es anterior al fin de una reserva existente
          AND @hora_solicitada <
              ADDTIME(
                  r.hora_inicio,
                  SEC_TO_TIME(r.duracion_minutos * 60)
              )

          -- y el nuevo fin es posterior al inicio existente
          AND ADDTIME(
                  @hora_solicitada,
                  SEC_TO_TIME(@duracion_solicitada * 60)
              ) > r.hora_inicio
    )
ORDER BY ca.nombre;



-- =========================================================
-- 5.3. HISTORIAL DE RESERVAS DE UN CLIENTE
-- Permite consultar las reservas realizadas por un cliente.
-- =========================================================

SET @email_cliente = 'juan.perez@example.com';

SELECT
    r.id_reserva,
    r.fecha,
    r.hora_inicio,
    d.nombre AS deporte,
    ca.nombre AS cancha,
    r.duracion_minutos,
    r.estado,
    r.precio_total
FROM reserva r
JOIN cliente c
    ON r.id_cliente = c.id_cliente
JOIN cancha ca
    ON r.id_cancha = ca.id_cancha
JOIN deporte d
    ON ca.id_deporte = d.id_deporte
WHERE c.email = @email_cliente
ORDER BY r.fecha DESC, r.hora_inicio DESC;



-- =========================================================
-- 5.4. HISTORIAL DE MOVIMIENTOS DE PAGO
-- Muestra señas, saldos y devoluciones de una reserva.
-- Relacionada con CU09 - Consultar movimientos de pago.
-- =========================================================

SET @id_reserva_consulta = 2;

SELECT
    r.id_reserva,
    CONCAT(c.nombre, ' ', c.apellido) AS cliente,
    p.tipo,
    p.importe,
    p.fecha_hora,
    u.nombre_usuario AS registrado_por
FROM pago p
JOIN reserva r
    ON p.id_reserva = r.id_reserva
JOIN cliente c
    ON r.id_cliente = c.id_cliente
JOIN usuario u
    ON p.id_usuario = u.id_usuario
WHERE r.id_reserva = @id_reserva_consulta
ORDER BY p.fecha_hora;



-- =========================================================
-- 5.5. TOTAL ABONADO POR RESERVA
-- Las señas y saldos suman.
-- Las devoluciones se descuentan.
-- =========================================================

SELECT
    r.id_reserva,
    CONCAT(c.nombre, ' ', c.apellido) AS cliente,
    r.precio_total,

    COALESCE(
        SUM(
            CASE
                WHEN p.tipo IN ('SENA', 'SALDO')
                    THEN p.importe
                WHEN p.tipo = 'DEVOLUCION'
                    THEN -p.importe
                ELSE 0
            END
        ),
        0
    ) AS total_abonado

FROM reserva r
JOIN cliente c
    ON r.id_cliente = c.id_cliente
LEFT JOIN pago p
    ON r.id_reserva = p.id_reserva
GROUP BY
    r.id_reserva,
    c.nombre,
    c.apellido,
    r.precio_total
ORDER BY r.id_reserva;



-- =========================================================
-- 5.6. INGRESOS NETOS POR PERÍODO
-- Considera señas y saldos como ingresos.
-- Las devoluciones se descuentan.
-- Relacionada con CU14 - Consultar reportes de gestión.
-- =========================================================

SET @fecha_desde = '2026-10-01 00:00:00';
SET @fecha_hasta = '2026-10-31 23:59:59';

SELECT
    DATE(p.fecha_hora) AS fecha,

    SUM(
        CASE
            WHEN p.tipo IN ('SENA', 'SALDO')
                THEN p.importe
            WHEN p.tipo = 'DEVOLUCION'
                THEN -p.importe
            ELSE 0
        END
    ) AS ingreso_neto

FROM pago p
WHERE p.fecha_hora BETWEEN @fecha_desde AND @fecha_hasta
GROUP BY DATE(p.fecha_hora)
ORDER BY fecha;



-- =========================================================
-- 5.7. CANTIDAD DE RESERVAS POR DEPORTE
-- Permite obtener información para reportes de ocupación.
-- Las reservas canceladas no se contabilizan.
-- =========================================================

SELECT
    d.nombre AS deporte,
    COUNT(r.id_reserva) AS cantidad_reservas
FROM deporte d
JOIN cancha ca
    ON ca.id_deporte = d.id_deporte
LEFT JOIN reserva r
    ON r.id_cancha = ca.id_cancha
   AND r.estado <> 'CANCELADA'
GROUP BY
    d.id_deporte,
    d.nombre
ORDER BY cantidad_reservas DESC, d.nombre;



-- =========================================================
-- 5.8. LISTADO GENERAL DE RESERVAS
-- Consulta útil para comprobar las relaciones entre tablas.
-- =========================================================

SELECT
    r.id_reserva,
    CONCAT(c.nombre, ' ', c.apellido) AS cliente,
    d.nombre AS deporte,
    ca.nombre AS cancha,
    r.fecha,
    r.hora_inicio,
    r.duracion_minutos,
    r.estado,
    r.precio_total,
    r.importe_sena,
    u.nombre_usuario AS registrada_por
FROM reserva r
JOIN cliente c
    ON r.id_cliente = c.id_cliente
JOIN cancha ca
    ON r.id_cancha = ca.id_cancha
JOIN deporte d
    ON ca.id_deporte = d.id_deporte
JOIN usuario u
    ON r.id_usuario = u.id_usuario
ORDER BY r.fecha, r.hora_inicio;

-- ==========================================
-- 6. BORRADO DE REGISTRO DE PRUEBA
-- ==========================================
    
INSERT INTO cliente (
    nombre,
    apellido,
    telefono,
    email,
    activo
)
VALUES (
    'Cliente',
    'Temporal',
    '11-0000-0000',
    'temporal@example.com',
    TRUE
);

SELECT *
FROM cliente
WHERE email = 'temporal@example.com';

DELETE FROM cliente
WHERE email = 'temporal@example.com';

SELECT *
FROM cliente
WHERE email = 'temporal@example.com';