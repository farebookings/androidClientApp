<?php
// ─── API ROUTER ──────────────────────────────────────────────
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PATCH, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit;
}

require_once __DIR__ . '/config/config.php';
require_once __DIR__ . '/controllers/AuthController.php';
require_once __DIR__ . '/controllers/BookingController.php';
require_once __DIR__ . '/controllers/DriverController.php';
require_once __DIR__ . '/controllers/AdminController.php';

$uri = parse_url($_SERVER['REQUEST_URI'], PHP_URL_PATH);
$uri = rtrim($uri, '/');
$method = $_SERVER['REQUEST_METHOD'];

// Simple router
try {
    switch (true) {
        // ─── AUTH ──────────────────────────────────────────
        case $uri === '/api/auth/register' && $method === 'POST':
            AuthController::register();
            break;

        case $uri === '/api/auth/login' && $method === 'POST':
            AuthController::login();
            break;

        case $uri === '/api/auth/profile' && $method === 'GET':
            AuthController::profile();
            break;

        // ─── BOOKINGS ──────────────────────────────────────
        case $uri === '/api/bookings' && $method === 'GET':
            BookingController::myBookings();
            break;

        case $uri === '/api/bookings' && $method === 'POST':
            BookingController::create();
            break;

        case $uri === '/api/bookings/estimate' && $method === 'POST':
            BookingController::estimate();
            break;

        case preg_match('#^/api/bookings/(\d+)$#', $uri, $m) && $method === 'GET':
            BookingController::get((int)$m[1]);
            break;

        case preg_match('#^/api/bookings/(\d+)/cancel$#', $uri, $m) && $method === 'PATCH':
            BookingController::cancel((int)$m[1]);
            break;

        case preg_match('#^/api/bookings/(\d+)/status$#', $uri, $m) && $method === 'PATCH':
            BookingController::updateStatus((int)$m[1]);
            break;

        // ─── DRIVERS ───────────────────────────────────────
        case $uri === '/api/drivers/register' && $method === 'POST':
            DriverController::register();
            break;

        case $uri === '/api/drivers/vehicle' && $method === 'POST':
            DriverController::registerVehicle();
            break;

        case $uri === '/api/drivers/location' && $method === 'POST':
            DriverController::updateLocation();
            break;

        case $uri === '/api/drivers/status' && $method === 'POST':
            DriverController::setStatus();
            break;

        case $uri === '/api/drivers/nearby' && $method === 'POST':
            DriverController::nearby();
            break;

        case $uri === '/api/drivers/bookings/pending' && $method === 'GET':
            DriverController::pendingBookings();
            break;

        case $uri === '/api/drivers/profile' && $method === 'GET':
            DriverController::profile();
            break;

        // ─── ADMIN ────────────────────────────────────────
        case $uri === '/api/admin/bookings' && $method === 'GET':
            AdminController::bookings();
            break;

        case $uri === '/api/admin/stats' && $method === 'GET':
            AdminController::stats();
            break;

        case $uri === '/api/admin/bookings/scheduled' && $method === 'GET':
            AdminController::scheduled();
            break;

        // ─── HEALTH ────────────────────────────────────────
        case $uri === '/api/health':
            echo json_encode(['status' => 'ok', 'time' => date('c')]);
            break;

        default:
            http_response_code(404);
            echo json_encode(['error' => 'Not found', 'path' => $uri]);
    }
} catch (Exception $e) {
    http_response_code(500);
    echo json_encode(['error' => $e->getMessage()]);
}
