-- ============================================================
-- TAXI APP DRIVER — Database Schema
-- MySQL 8.0+ / PHP 8.3 PDO
-- ============================================================

CREATE DATABASE IF NOT EXISTS taxi_app CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE taxi_app;

-- ─── USERS ───────────────────────────────────────────────────
CREATE TABLE users (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(150) NOT NULL UNIQUE,
    phone       VARCHAR(20) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    role        ENUM('client', 'driver', 'admin') NOT NULL DEFAULT 'client',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ─── DRIVERS ────────────────────────────────────────────────
CREATE TABLE drivers (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL UNIQUE,
    license_number  VARCHAR(50) NOT NULL,
    status          ENUM('available', 'busy', 'offline') NOT NULL DEFAULT 'offline',
    current_lat     DECIMAL(10, 7) NULL,
    current_lng     DECIMAL(10, 7) NULL,
    last_location   TIMESTAMP NULL,
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─── VEHICLES ───────────────────────────────────────────────
CREATE TABLE vehicles (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    driver_id   INT NOT NULL UNIQUE,
    make        VARCHAR(50) NOT NULL,
    model       VARCHAR(50) NOT NULL,
    plate       VARCHAR(15) NOT NULL,
    color       VARCHAR(30) NOT NULL,
    seats       TINYINT NOT NULL DEFAULT 4,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (driver_id) REFERENCES drivers(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─── BOOKINGS ───────────────────────────────────────────────
CREATE TABLE bookings (
    id                  INT AUTO_INCREMENT PRIMARY KEY,
    client_id           INT NOT NULL,
    driver_id           INT NULL,
    pickup_address      VARCHAR(255) NOT NULL,
    pickup_lat          DECIMAL(10, 7) NOT NULL,
    pickup_lng          DECIMAL(10, 7) NOT NULL,
    dropoff_address     VARCHAR(255) NOT NULL,
    dropoff_lat         DECIMAL(10, 7) NOT NULL,
    dropoff_lng         DECIMAL(10, 7) NOT NULL,
    type                ENUM('immediate', 'scheduled') NOT NULL,
    scheduled_date      DATETIME NULL,
    status              ENUM('pending','confirmed','in_progress','completed','cancelled') NOT NULL DEFAULT 'pending',
    fare                DECIMAL(10, 2) NULL,
    notes               TEXT NULL,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (driver_id) REFERENCES users(id) ON DELETE SET NULL
) ENGINE=InnoDB;

-- ─── DRIVER LOCATIONS (tracking) ────────────────────────────
CREATE TABLE driver_locations (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    driver_id   INT NOT NULL,
    lat         DECIMAL(10, 7) NOT NULL,
    lng         DECIMAL(10, 7) NOT NULL,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (driver_id) REFERENCES drivers(id) ON DELETE CASCADE,
    INDEX idx_location (lat, lng),
    INDEX idx_driver (driver_id)
) ENGINE=InnoDB;

-- ─── NOTIFICATIONS ─────────────────────────────────────────
CREATE TABLE notifications (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    user_id     INT NOT NULL,
    booking_id  INT NULL,
    title       VARCHAR(255) NOT NULL,
    message     TEXT NOT NULL,
    is_read     TINYINT(1) DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ─── ADMIN USER (default) ──────────────────────────────────
-- Password: admin123 (bcrypt hash)
INSERT INTO users (name, email, phone, password, role) VALUES
('Admin', 'admin@taxi.com', '+000000000', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'admin');
