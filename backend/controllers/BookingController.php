<?php
require_once __DIR__ . '/../models/Booking.php';
require_once __DIR__ . '/../models/Driver.php';
require_once __DIR__ . '/../middleware/auth.php';

class BookingController {
    /**
     * Create a new booking
     * POST /bookings
     */
    public static function create(): void {
        $auth = Auth::requireAuth();
        $input = json_decode(file_get_contents('php://input'), true);

        $required = ['pickup_address', 'pickup_lat', 'pickup_lng', 'dropoff_address', 'dropoff_lat', 'dropoff_lng', 'type'];
        foreach ($required as $field) {
            if (!isset($input[$field])) {
                http_response_code(400);
                echo json_encode(['error' => "Missing field: $field"]);
                return;
            }
        }

        $input['client_id'] = $auth['user_id'];

        if ($input['type'] === 'immediate') {
            // Find nearest available driver if not specified
            if (empty($input['driver_id'])) {
                $nearby = Driver::findNearby($input['pickup_lat'], $input['pickup_lng']);
                if (!empty($nearby)) {
                    $input['driver_id'] = (int)$nearby[0]['user_id'];
                }
            }
        }

        try {
            $booking = Booking::create($input);
            echo json_encode(['booking' => $booking]);
        } catch (Exception $e) {
            http_response_code(500);
            echo json_encode(['error' => $e->getMessage()]);
        }
    }

    /**
     * Get my bookings (client)
     * GET /bookings
     */
    public static function myBookings(): void {
        $auth = Auth::requireAuth();
        $bookings = Booking::getByClient($auth['user_id']);
        echo json_encode(['bookings' => $bookings]);
    }

    /**
     * Get booking details
     * GET /bookings/{id}
     */
    public static function get(int $id): void {
        Auth::requireAuth();
        $booking = Booking::getById($id);
        if (!$booking) {
            http_response_code(404);
            echo json_encode(['error' => 'Booking not found']);
            return;
        }
        echo json_encode(['booking' => $booking]);
    }

    /**
     * Cancel a booking
     * PATCH /bookings/{id}/cancel
     */
    public static function cancel(int $id): void {
        $auth = Auth::requireAuth();
        $booking = Booking::getById($id);

        if (!$booking) {
            http_response_code(404);
            echo json_encode(['error' => 'Booking not found']);
            return;
        }

        if ((int)$booking['client_id'] !== $auth['user_id'] && $auth['role'] !== 'admin') {
            http_response_code(403);
            echo json_encode(['error' => 'Not your booking']);
            return;
        }

        Booking::updateStatus($id, 'cancelled');
        echo json_encode(['message' => 'Booking cancelled']);
    }

    /**
     * Update booking status (driver accepts)
     * PATCH /bookings/{id}/status
     */
    public static function updateStatus(int $id): void {
        $auth = Auth::requireAuth();
        $input = json_decode(file_get_contents('php://input'), true);

        if (empty($input['status'])) {
            http_response_code(400);
            echo json_encode(['error' => 'status required']);
            return;
        }

        $driverId = ($auth['role'] === 'driver' || $auth['role'] === 'admin')
            ? $auth['user_id'] : null;

        Booking::updateStatus($id, $input['status'], $driverId);
        echo json_encode(['message' => 'Status updated']);
    }

    /**
     * Estimate fare
     * POST /bookings/estimate
     */
    public static function estimate(): void {
        $input = json_decode(file_get_contents('php://input'), true);

        if (!isset($input['pickup_lat'], $input['pickup_lng'], $input['dropoff_lat'], $input['dropoff_lng'])) {
            http_response_code(400);
            echo json_encode(['error' => 'pickup and dropoff coordinates required']);
            return;
        }

        $fare = Booking::calculateFare(
            $input['pickup_lat'], $input['pickup_lng'],
            $input['dropoff_lat'], $input['dropoff_lng']
        );

        echo json_encode(['fare' => $fare]);
    }
}
