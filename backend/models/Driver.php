<?php
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../config/config.php';

class Driver {
    /**
     * Register a driver profile
     */
    public static function register(int $userId, string $licenseNumber): array {
        $db = Database::getConnection();
        $stmt = $db->prepare("INSERT INTO drivers (user_id, license_number) VALUES (?, ?)");
        $stmt->execute([$userId, $licenseNumber]);
        return self::getByUserId($userId);
    }

    /**
     * Register vehicle for a driver
     */
    public static function registerVehicle(int $driverId, string $make, string $model, string $plate, string $color, int $seats): array {
        $db = Database::getConnection();
        $stmt = $db->prepare(
            "INSERT INTO vehicles (driver_id, make, model, plate, color, seats) VALUES (?, ?, ?, ?, ?, ?)"
        );
        $stmt->execute([$driverId, $make, $model, $plate, $color, $seats]);
        return self::getVehicle($driverId);
    }

    public static function getByUserId(int $userId): ?array {
        $db = Database::getConnection();
        $stmt = $db->prepare("SELECT * FROM drivers WHERE user_id = ?");
        $stmt->execute([$userId]);
        return $stmt->fetch() ?: null;
    }

    public static function getVehicle(int $driverId): ?array {
        $db = Database::getConnection();
        $stmt = $db->prepare("SELECT * FROM vehicles WHERE driver_id = ?");
        $stmt->execute([$driverId]);
        return $stmt->fetch() ?: null;
    }

    /**
     * Update driver location
     */
    public static function updateLocation(int $driverId, float $lat, float $lng): void {
        $db = Database::getConnection();
        $stmt = $db->prepare(
            "UPDATE drivers SET current_lat = ?, current_lng = ?, last_location = NOW(), status = 'available' WHERE id = ?"
        );
        $stmt->execute([$lat, $lng, $driverId]);

        // Also log to driver_locations for history
        $stmt2 = $db->prepare("INSERT INTO driver_locations (driver_id, lat, lng) VALUES (?, ?, ?)");
        $stmt2->execute([$driverId, $lat, $lng]);
    }

    /**
     * Find nearby available drivers
     */
    public static function findNearby(float $lat, float $lng, float $radiusKm = null): array {
        $db = Database::getConnection();
        $radius = $radiusKm ?? NEARBY_RADIUS_KM;

        // Haversine formula in SQL
        $stmt = $db->prepare("
            SELECT d.*, u.name AS driver_name, u.phone, v.make, v.model, v.plate, v.color, v.seats,
                   (6371 * acos(cos(radians(?)) * cos(radians(d.current_lat))
                    * cos(radians(d.current_lng) - radians(?))
                    + sin(radians(?)) * sin(radians(d.current_lat)))) AS distance
            FROM drivers d
            JOIN users u ON u.id = d.user_id
            LEFT JOIN vehicles v ON v.driver_id = d.id
            WHERE d.status = 'available'
              AND d.current_lat IS NOT NULL
              AND d.current_lng IS NOT NULL
            HAVING distance <= ?
            ORDER BY distance ASC
        ");
        $stmt->execute([$lat, $lng, $lat, $radius]);
        return $stmt->fetchAll();
    }

    /**
     * Update driver status
     */
    public static function setStatus(int $driverId, string $status): void {
        $db = Database::getConnection();
        $stmt = $db->prepare("UPDATE drivers SET status = ? WHERE id = ?");
        $stmt->execute([$status, $driverId]);
    }

    public static function getById(int $id): ?array {
        $db = Database::getConnection();
        $stmt = $db->prepare("
            SELECT d.*, u.name AS driver_name, u.email, u.phone
            FROM drivers d
            JOIN users u ON u.id = d.user_id
            WHERE d.id = ?
        ");
        $stmt->execute([$id]);
        return $stmt->fetch() ?: null;
    }
}
