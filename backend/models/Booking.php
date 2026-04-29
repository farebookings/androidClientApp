<?php
require_once __DIR__ . '/../config/database.php';
require_once __DIR__ . '/../config/config.php';

class Booking {
    /**
     * Create a new booking (immediate or scheduled)
     */
    public static function create(array $data): array {
        $db = Database::getConnection();

        $stmt = $db->prepare("
            INSERT INTO bookings
                (client_id, driver_id, pickup_address, pickup_lat, pickup_lng,
                 dropoff_address, dropoff_lat, dropoff_lng, type, scheduled_date, status, fare, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ");

        $driverId = $data['driver_id'] ?? null;
        $scheduledDate = ($data['type'] === 'scheduled') ? ($data['scheduled_date'] ?? null) : null;
        $fare = $data['fare'] ?? self::calculateFare($data['pickup_lat'], $data['pickup_lng'],
                                                      $data['dropoff_lat'], $data['dropoff_lng']);
        $status = $data['type'] === 'immediate' ? 'pending' : 'pending';
        $notes = $data['notes'] ?? null;

        $stmt->execute([
            $data['client_id'],
            $driverId,
            $data['pickup_address'],
            $data['pickup_lat'],
            $data['pickup_lng'],
            $data['dropoff_address'],
            $data['dropoff_lat'],
            $data['dropoff_lng'],
            $data['type'],
            $scheduledDate,
            $status,
            $fare,
            $notes,
        ]);

        $bookingId = (int)$db->lastInsertId();
        return self::getById($bookingId);
    }

    /**
     * Get booking by ID
     */
    public static function getById(int $id): ?array {
        $db = Database::getConnection();
        $stmt = $db->prepare("
            SELECT b.*,
                   c.name AS client_name, c.phone AS client_phone,
                   d.name AS driver_name, d.phone AS driver_phone,
                   v.make AS car_make, v.model AS car_model, v.plate AS car_plate, v.color AS car_color
            FROM bookings b
            JOIN users c ON c.id = b.client_id
            LEFT JOIN users d ON d.id = b.driver_id
            LEFT JOIN drivers dr ON dr.user_id = b.driver_id
            LEFT JOIN vehicles v ON v.driver_id = dr.id
            WHERE b.id = ?
        ");
        $stmt->execute([$id]);
        return $stmt->fetch() ?: null;
    }

    /**
     * Get bookings for a client
     */
    public static function getByClient(int $clientId, int $limit = 20): array {
        $db = Database::getConnection();
        $stmt = $db->prepare("
            SELECT b.*, d.name AS driver_name,
                   v.make AS car_make, v.model AS car_model, v.plate AS car_plate
            FROM bookings b
            LEFT JOIN users d ON d.id = b.driver_id
            LEFT JOIN drivers dr ON dr.user_id = b.driver_id
            LEFT JOIN vehicles v ON v.driver_id = dr.id
            WHERE b.client_id = ?
            ORDER BY b.created_at DESC
            LIMIT ?
        ");
        $stmt->execute([$clientId, $limit]);
        return $stmt->fetchAll();
    }

    /**
     * Get pending bookings for drivers
     */
    public static function getPendingForDrivers(int $limit = 20): array {
        $db = Database::getConnection();
        $stmt = $db->prepare("
            SELECT b.*, c.name AS client_name, c.phone AS client_phone
            FROM bookings b
            JOIN users c ON c.id = b.client_id
            WHERE b.status = 'pending'
              AND b.type = 'immediate'
            ORDER BY b.created_at ASC
            LIMIT ?
        ");
        $stmt->execute([$limit]);
        return $stmt->fetchAll();
    }

    /**
     * Get scheduled bookings (for admin panel)
     */
    public static function getScheduled(int $limit = 50): array {
        $db = Database::getConnection();
        $stmt = $db->prepare("
            SELECT b.*, c.name AS client_name, c.phone AS client_phone, c.email AS client_email,
                   d.name AS driver_name
            FROM bookings b
            JOIN users c ON c.id = b.client_id
            LEFT JOIN users d ON d.id = b.driver_id
            WHERE b.type = 'scheduled'
            ORDER BY b.scheduled_date ASC
            LIMIT ?
        ");
        $stmt->execute([$limit]);
        return $stmt->fetchAll();
    }

    /**
     * Update booking status
     */
    public static function updateStatus(int $bookingId, string $status, ?int $driverId = null): void {
        $db = Database::getConnection();
        if ($driverId !== null) {
            $stmt = $db->prepare("UPDATE bookings SET status = ?, driver_id = ? WHERE id = ?");
            $stmt->execute([$status, $driverId, $bookingId]);
        } else {
            $stmt = $db->prepare("UPDATE bookings SET status = ? WHERE id = ?");
            $stmt->execute([$status, $bookingId]);
        }
    }

    /**
     * Simple fare estimation based on distance
     */
    public static function calculateFare(float $pickupLat, float $pickupLng, float $dropoffLat, float $dropoffLng): float {
        // Approximate distance using haversine
        $earthRadius = 6371;
        $dLat = deg2rad($dropoffLat - $pickupLat);
        $dLng = deg2rad($dropoffLng - $pickupLng);
        $a = sin($dLat / 2) ** 2 + cos(deg2rad($pickupLat)) * cos(deg2rad($dropoffLat)) * sin($dLng / 2) ** 2;
        $c = 2 * atan2(sqrt($a), sqrt(1 - $a));
        $distanceKm = $earthRadius * $c;

        $estimatedMinutes = max(5, $distanceKm * 3); // ~3 min per km

        return round(BASE_FARE + ($distanceKm * PER_KM_FARE) + ($estimatedMinutes * PER_MIN_FARE), 2);
    }

    /**
     * Get all bookings (admin)
     */
    public static function getAll(int $limit = 50, int $offset = 0): array {
        $db = Database::getConnection();
        $stmt = $db->prepare("
            SELECT b.*, c.name AS client_name, c.phone AS client_phone,
                   d.name AS driver_name
            FROM bookings b
            JOIN users c ON c.id = b.client_id
            LEFT JOIN users d ON d.id = b.driver_id
            ORDER BY b.created_at DESC
            LIMIT ? OFFSET ?
        ");
        $stmt->execute([$limit, $offset]);
        return $stmt->fetchAll();
    }

    public static function countAll(): int {
        $db = Database::getConnection();
        return (int)$db->query("SELECT COUNT(*) FROM bookings")->fetchColumn();
    }
}
