<?php

declare(strict_types=1);

use App\Api\ApiClient;

session_name('tma_frontend');
session_start();

require_once __DIR__ . '/helpers.php';
require_once __DIR__ . '/ApiClient.php';
require_once __DIR__ . '/View.php';

$apiBaseUrl = rtrim((string) (getenv('SPRING_API_BASE_URL') ?: 'http://localhost:8080'), '/');

return [
    'apiClient' => new ApiClient($apiBaseUrl),
];
