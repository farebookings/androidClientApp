<?php
require_once __DIR__ . '/../config/config.php';

class Auth {
    /**
     * Generate JWT token
     */
    public static function generateToken(int $userId, string $role): string {
        $header = self::base64Url(['alg' => 'HS256', 'typ' => 'JWT']);
        $payload = self::base64Url([
            'user_id' => $userId,
            'role'    => $role,
            'iat'     => time(),
            'exp'     => time() + JWT_EXPIRY,
        ]);
        $signature = self::base64Url(
            hash_hmac('sha256', "$header.$payload", JWT_SECRET, true)
        );
        return "$header.$payload.$signature";
    }

    /**
     * Validate JWT and return payload or null
     */
    public static function validateToken(string $token): ?array {
        $parts = explode('.', $token);
        if (count($parts) !== 3) return null;

        [$header, $payload, $signature] = $parts;
        $expected = self::base64Url(
            hash_hmac('sha256', "$header.$payload", JWT_SECRET, true)
        );

        if (!hash_equals($expected, $signature)) return null;

        $data = json_decode(self::urlDecode($payload), true);
        if (!$data || !isset($data['exp']) || $data['exp'] < time()) return null;

        return $data;
    }

    /**
     * Require authentication — returns user data or exits
     */
    public static function requireAuth(): array {
        $headers = getallheaders();
        $auth = $headers['Authorization'] ?? $headers['authorization'] ?? '';

        if (!preg_match('/^Bearer\s+(.+)$/', $auth, $matches)) {
            http_response_code(401);
            echo json_encode(['error' => 'Token required']);
            exit;
        }

        $payload = self::validateToken($matches[1]);
        if (!$payload) {
            http_response_code(401);
            echo json_encode(['error' => 'Invalid or expired token']);
            exit;
        }

        return $payload;
    }

    /**
     * Require admin role
     */
    public static function requireAdmin(): array {
        $payload = self::requireAuth();
        if ($payload['role'] !== 'admin') {
            http_response_code(403);
            echo json_encode(['error' => 'Admin access required']);
            exit;
        }
        return $payload;
    }

    private static function base64Url(array $data): string {
        return rtrim(strtr(base64_encode(json_encode($data)), '+/', '-_'), '=');
    }

    private static function urlDecode(string $data): string {
        return base64_decode(strtr($data, '-_', '+/'));
    }
}
