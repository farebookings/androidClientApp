<?php
require_once __DIR__ . '/../models/Booking.php';
require_once __DIR__ . '/../middleware/auth.php';

class AdminController {
    public static function bookings(): void {
        Auth::requireAdmin();

        $limit = (int)($_GET['limit'] ?? 50);
        $offset = (int)($_GET['offset'] ?? 0);
        $type = $_GET['type'] ?? null;
        $status = $_GET['status'] ?? null;

        $bookings = Booking::getAll($limit, $offset);

        // Filter in-memory for simplicity (or add SQL filters for production)
        if ($type && $type !== 'all') {
            $bookings = array_filter($bookings, fn($b) => $b['type'] === $type);
        }
        if ($status && $status !== 'all') {
            $bookings = array_filter($bookings, fn($b) => $b['status'] === $status);
        }

        $bookings = array_values($bookings);

        echo json_encode(['bookings' => $bookings, 'total' => Booking::countAll()]);
    }

    public static function stats(): void {
        Auth::requireAdmin();
        $db = \Database::getConnection();

        $stats = [
            'total'       => (int)$db->query("SELECT COUNT(*) FROM bookings")->fetchColumn(),
            'pending'     => (int)$db->query("SELECT COUNT(*) FROM bookings WHERE status='pending'")->fetchColumn(),
            'scheduled'   => (int)$db->query("SELECT COUNT(*) FROM bookings WHERE type='scheduled'")->fetchColumn(),
            'in_progress' => (int)$db->query("SELECT COUNT(*) FROM bookings WHERE status='in_progress'")->fetchColumn(),
            'completed'   => (int)$db->query("SELECT COUNT(*) FROM bookings WHERE status='completed'")->fetchColumn(),
            'drivers'     => (int)$db->query("SELECT COUNT(*) FROM drivers")->fetchColumn(),
            'clients'     => (int)$db->query("SELECT COUNT(*) FROM users WHERE role='client'")->fetchColumn(),
        ];

        echo json_encode(['stats' => $stats]);
    }

    public static function scheduled(): void {
        Auth::requireAdmin();
        $bookings = Booking::getScheduled();
        echo json_encode(['bookings' => $bookings]);
    }
}
