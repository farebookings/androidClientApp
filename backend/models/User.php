<?php
require_once __DIR__ . '/../config/database.php';

class User {
    public static function register(string $name, string $email, string $phone, string $password, string $role = 'client'): array {
        $db = Database::getConnection();

        $stmt = $db->prepare("SELECT id FROM users WHERE email = ?");
        $stmt->execute([$email]);
        if ($stmt->fetch()) {
            throw new RuntimeException('Email already registered');
        }

        $hash = password_hash($password, PASSWORD_BCRYPT);
        $stmt = $db->prepare(
            "INSERT INTO users (name, email, phone, password, role) VALUES (?, ?, ?, ?, ?)"
        );
        $stmt->execute([$name, $email, $phone, $hash, $role]);

        $userId = (int)$db->lastInsertId();

        return [
            'id'    => $userId,
            'name'  => $name,
            'email' => $email,
            'phone' => $phone,
            'role'  => $role,
        ];
    }

    public static function login(string $email, string $password): ?array {
        $db = Database::getConnection();
        $stmt = $db->prepare("SELECT * FROM users WHERE email = ?");
        $stmt->execute([$email]);
        $user = $stmt->fetch();

        if (!$user || !password_verify($password, $user['password'])) {
            return null;
        }

        return $user;
    }

    public static function getById(int $id): ?array {
        $db = Database::getConnection();
        $stmt = $db->prepare("SELECT id, name, email, phone, role, created_at FROM users WHERE id = ?");
        $stmt->execute([$id]);
        return $stmt->fetch() ?: null;
    }
}
