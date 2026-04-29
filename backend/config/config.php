<?php
// ─── APPLICATION CONFIGURATION ───────────────────────────────
define('DB_HOST', getenv('DB_HOST') ?: 'localhost');
define('DB_NAME', getenv('DB_NAME') ?: 'taxi_app');
define('DB_USER', getenv('DB_USER') ?: 'root');
define('DB_PASS', getenv('DB_PASS') ?: '');
define('JWT_SECRET', getenv('JWT_SECRET') ?: 'change_this_to_a_random_secret_key_2026');
define('JWT_EXPIRY', 86400 * 7); // 7 days
define('NEARBY_RADIUS_KM', 3.0); // find drivers within 3km
define('BASE_FARE', 3.50);
define('PER_KM_FARE', 1.20);
define('PER_MIN_FARE', 0.35);
