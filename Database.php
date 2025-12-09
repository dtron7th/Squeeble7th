<?php
// create_db.php
require_once __DIR__ . '/Database.php';

header('Content-Type: application/json');

$dbName = $_POST['dbname'] ?? null;
if (!$dbName) {
    http_response_code(400);
    echo json_encode(['ok' => false, 'error' => 'Missing dbname']);
    exit;
}

try {
    // change host/user/pass/port if needed
    $db = new Database('127.0.0.1', 'root', 'secret', 3306);
    $created = $db->createDatabase($dbName);
    echo json_encode(['ok' => true, 'created' => (bool)$created]);
} catch (Throwable $e) {
    http_response_code(500);
    echo json_encode(['ok' => false, 'error' => $e->getMessage()]);
}
