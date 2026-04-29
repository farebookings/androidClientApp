<?php
require_once __DIR__ . '/../models/Driver.php';
require_once __DIR__ . '/../models/Booking.php';
require_once __DIR__ . '/../middleware/auth.php';

class DriverController {
    /**
     * Register as a driver
     * POST /drivers/register
     */
    public static function register(): void {
        $auth = Auth::requireAuth();
        if ($auth['role'] !== 'driver') {
            http_response_code(403);
            echo json_encode(['error' => 'User must have driver role']);
            return;
        }

        $input = json_decode(file_get_contents('php://input'), true);
        if (empty($input['license_number'])) {
            http_response_code(400);
            echo json_encode(['error' => 'license_number required']);
            return;
        }

        $driver = Driver::register($auth['user_id'], $input['license_number']);
        echo json_encode(['driver' => $driver]);
    }

    /**
     * Register vehicle
     * POST /drivers/vehicle
     */
    public static function registerVehicle(): void {
        $auth = Auth::requireAuth();
        $driver = Driver::getByUserId($auth['user_id']);

        if (!$driver) {
            http_response_code(400);
            echo json_encode(['error' => 'Register as driver first']);
            return;
        }

        $input = json_decode(file_get_contents('php://input'), true);
        $required = ['make', 'model', 'plate', 'color'];
        foreach ($required as $f) {
            if (empty($input[$f])) {
                http_response_code(400);
                echo json_encode(['error' => "$f required"]);
                return;
            }
        }

        $vehicle = Driver::registerVehicle(
            (int)$driver['id'], $input['make'], $input['model'],
            $input['plate'], $input['color'], (int)($input['seats'] ?? 4)
        );
        echo json_encode(['vehicle' => $vehicle]);
    }

    /**
     * Update driver location
     * POST /drivers/location
     */
    public static function updateLocation(): void {
        $auth = Auth::requireAuth();
        $driver = Driver::getByUserId($auth['user_id']);

        if (!$driver) {
            http_response_code(400);
            echo json_encode(['error' => 'Driver profile not found']);
            return;
        }

        $input = json_decode(file_get_contents('php://input'), true);
        if (!isset($input['lat'], $input['lng'])) {
            http_response_code(400);
            echo json_encode(['error' => 'lat and lng required']);
            return;
        }

        Driver::updateLocation((int)$driver['id'], (float)$input['lat'], (float)$input['lng']);
        echo json_encode(['message' => 'Location updated']);
    }

    /**
     * Set driver status
     * POST /drivers/status
     */
    public static function setStatus(): void {
        $auth = Auth::requireAuth();
        $driver = Driver::getByUserId($auth['user_id']);

        if (!$driver) {
            http_response_code(400);
            echo json_encode(['error' => 'Driver profile not found']);
            return;
        }

        $input = json_decode(file_get_contents('php://input'), true);
        $validStatuses = ['available', 'busy', 'offline'];
        if (!in_array($input['status'] ?? '', $validStatuses)) {
            http_response_code(400);
            echo json_encode(['error' => 'Invalid status: ' . implode(', ', $validStatuses)]);
            return;
        }

        Driver::setStatus((int)$driver['id'], $input['status']);
        echo json_encode(['message' => 'Status updated']);
    }

    /**
     * Find nearby drivers
     * POST /drivers/nearby
     */
    public static function nearby(): void {
        $input = json_decode(file_get_contents('php://input'), true);
        if (!isset($input['lat'], $input['lng'])) {
            http_response_code(400);
            echo json_encode(['error' => 'lat and lng required']);
            return;
        }

        $drivers = Driver::findNearby((float)$input['lat'], (float)$input['lng']);
        echo json_encode(['drivers' => $drivers]);
    }

    /**
     * Get pending bookings (for driver app)
     * GET /drivers/bookings/pending
     */
    public static function pendingBookings(): void {
        Auth::requireAuth();
        $bookings = Booking::getPendingForDrivers();
        echo json_encode(['bookings' => $bookings]);
    }

    /**
     * Get driver profile
     * GET /drivers/profile
     */
    public static function profile(): void {
        $auth = Auth::requireAuth();
        $driver = Driver::getByUserId($auth['user_id']);
        $vehicle = $driver ? Driver::getVehicle((int)$driver['id']) : null;

        echo json_encode([
            'driver'  => $driver,
            'vehicle' => $vehicle,
        ]);
    }
}
