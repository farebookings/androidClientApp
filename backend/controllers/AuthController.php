<?php
require_once __DIR__ . '/../models/User.php';
require_once __DIR__ . '/../middleware/auth.php';

class AuthController {
    public static function register(): void {
        $input = json_decode(file_get_contents('php://input'), true);

        if (empty($input['name']) || empty($input['email']) || empty($input['password']) || empty($input['phone'])) {
            http_response_code(400);
            echo json_encode(['error' => 'name, email, phone and password required']);
            return;
        }

        try {
            $user = User::register(
                $input['name'],
                $input['email'],
                $input['phone'],
                $input['password'],
                $input['role'] ?? 'client'
            );
            $token = Auth::generateToken($user['id'], $user['role']);
            echo json_encode(['user' => $user, 'token' => $token]);
        } catch (RuntimeException $e) {
            http_response_code(409);
            echo json_encode(['error' => $e->getMessage()]);
        }
    }

    public static function login(): void {
        $input = json_decode(file_get_contents('php://input'), true);

        if (empty($input['email']) || empty($input['password'])) {
            http_response_code(400);
            echo json_encode(['error' => 'email and password required']);
            return;
        }

        $user = User::login($input['email'], $input['password']);
        if (!$user) {
            http_response_code(401);
            echo json_encode(['error' => 'Invalid credentials']);
            return;
        }

        $token = Auth::generateToken((int)$user['id'], $user['role']);
        echo json_encode([
            'user' => [
                'id'    => (int)$user['id'],
                'name'  => $user['name'],
                'email' => $user['email'],
                'phone' => $user['phone'],
                'role'  => $user['role'],
            ],
            'token' => $token,
        ]);
    }

    public static function profile(): void {
        $auth = Auth::requireAuth();
        $user = User::getById($auth['user_id']);
        if (!$user) {
            http_response_code(404);
            echo json_encode(['error' => 'User not found']);
            return;
        }
        echo json_encode(['user' => $user]);
    }
}
