<?php
// ─── DATABASE SETUP SCRIPT ──────────────────────────────────
// Run this once to create the database and tables
// Usage: php setup.php
// Or access via web: https://bookings.toronet.es/setup.php (then DELETE this file)

$host = getenv('DB_HOST') ?: 'localhost';
$user = getenv('DB_USER') ?: 'bookings_toronet';
$pass = getenv('DB_PASS') ?: 'tu_contraseña_aqui';

$dbName = 'bookings_toronet';

try {
    // First connect without database to create it
    $pdo = new PDO("mysql:host=$host;charset=utf8mb4", $user, $pass, [
        PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
    ]);

    $pdo->exec("CREATE DATABASE IF NOT EXISTS `$dbName` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
    $pdo->exec("USE `$dbName`");

    // ─── TABLES ───────────────────────────────────────────
    $pdo->exec("
        CREATE TABLE IF NOT EXISTS users (
            id INT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR(100) NOT NULL,
            email VARCHAR(150) NOT NULL UNIQUE,
            phone VARCHAR(20) NOT NULL,
            password VARCHAR(255) NOT NULL,
            role ENUM('client', 'driver', 'admin') NOT NULL DEFAULT 'client',
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ) ENGINE=InnoDB
    ");

    $pdo->exec("
        CREATE TABLE IF NOT EXISTS drivers (
            id INT AUTO_INCREMENT PRIMARY KEY,
            user_id INT NOT NULL UNIQUE,
            license_number VARCHAR(50) NOT NULL,
            status ENUM('available', 'busy', 'offline') NOT NULL DEFAULT 'offline',
            current_lat DECIMAL(10, 7) NULL,
            current_lng DECIMAL(10, 7) NULL,
            last_location TIMESTAMP NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        ) ENGINE=InnoDB
    ");

    $pdo->exec("
        CREATE TABLE IF NOT EXISTS vehicles (
            id INT AUTO_INCREMENT PRIMARY KEY,
            driver_id INT NOT NULL UNIQUE,
            make VARCHAR(50) NOT NULL,
            model VARCHAR(50) NOT NULL,
            plate VARCHAR(15) NOT NULL,
            color VARCHAR(30) NOT NULL,
            seats TINYINT NOT NULL DEFAULT 4,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (driver_id) REFERENCES drivers(id) ON DELETE CASCADE
        ) ENGINE=InnoDB
    ");

    $pdo->exec("
        CREATE TABLE IF NOT EXISTS bookings (
            id INT AUTO_INCREMENT PRIMARY KEY,
            client_id INT NOT NULL,
            driver_id INT NULL,
            pickup_address VARCHAR(255) NOT NULL,
            pickup_lat DECIMAL(10, 7) NOT NULL,
            pickup_lng DECIMAL(10, 7) NOT NULL,
            dropoff_address VARCHAR(255) NOT NULL,
            dropoff_lat DECIMAL(10, 7) NOT NULL,
            dropoff_lng DECIMAL(10, 7) NOT NULL,
            type ENUM('immediate', 'scheduled') NOT NULL,
            scheduled_date DATETIME NULL,
            status ENUM('pending','confirmed','in_progress','completed','cancelled') NOT NULL DEFAULT 'pending',
            fare DECIMAL(10, 2) NULL,
            notes TEXT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE CASCADE,
            FOREIGN KEY (driver_id) REFERENCES users(id) ON DELETE SET NULL
        ) ENGINE=InnoDB
    ");

    $pdo->exec("
        CREATE TABLE IF NOT EXISTS driver_locations (
            id INT AUTO_INCREMENT PRIMARY KEY,
            driver_id INT NOT NULL,
            lat DECIMAL(10, 7) NOT NULL,
            lng DECIMAL(10, 7) NOT NULL,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            FOREIGN KEY (driver_id) REFERENCES drivers(id) ON DELETE CASCADE,
            INDEX idx_location (lat, lng),
            INDEX idx_driver (driver_id)
        ) ENGINE=InnoDB
    ");

    $pdo->exec("
        CREATE TABLE IF NOT EXISTS notifications (
            id INT AUTO_INCREMENT PRIMARY KEY,
            user_id INT NOT NULL,
            booking_id INT NULL,
            title VARCHAR(255) NOT NULL,
            message TEXT NOT NULL,
            is_read TINYINT(1) DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        ) ENGINE=InnoDB
    ");

    // Default admin user (password: admin123)
    $hash = password_hash('admin123', PASSWORD_BCRYPT);
    $stmt = $pdo->prepare("INSERT IGNORE INTO users (name, email, phone, password, role) VALUES (?, ?, ?, ?, ?)");
    $stmt->execute(['Admin', 'admin@taxi.com', '+000000000', $hash, 'admin']);

    echo "✅ Database '$dbName' created successfully!\n";
    echo "✅ All tables created\n";
    echo "✅ Admin user: admin@taxi.com / admin123\n";
    echo "\n⚠️  DELETE THIS FILE AFTER USE!\n";

} catch (PDOException $e) {
    echo "❌ Error: " . $e->getMessage() . "\n";
    echo "\nTry creating the database manually in Plesk:\n";
    echo "  1. Go to Plesk > Databases > Add Database\n";
    echo "  2. Database name: $dbName\n";
    echo "  3. Create a database user with all privileges\n";
    echo "  4. Then update config.php with those credentials\n";
}
