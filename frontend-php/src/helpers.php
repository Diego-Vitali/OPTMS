<?php

declare(strict_types=1);

function route_path(): string
{
    $uri = (string) ($_SERVER['REQUEST_URI'] ?? '/');
    $path = parse_url($uri, PHP_URL_PATH);
    return $path ? rtrim($path, '/') ?: '/' : '/';
}

function redirect(string $path): never
{
    header('Location: ' . $path);
    exit;
}

function e(?string $value): string
{
    return htmlspecialchars((string) $value, ENT_QUOTES, 'UTF-8');
}

function asset_path(string $path): string
{
    return '/assets/' . ltrim($path, '/');
}

function app_name(): string
{
    return 'OPTMS';
}

function app_slogan(): string
{
    return 'OPTMS: A tecnologia que otimiza suas entregas e conecta seu frete ao mercado.';
}

function flash(string $type, string $message): void
{
    $_SESSION['_flash_messages'][] = [
        'type' => $type,
        'message' => $message,
    ];
}

function consume_flashes(): array
{
    $messages = $_SESSION['_flash_messages'] ?? [];
    unset($_SESSION['_flash_messages']);
    return is_array($messages) ? $messages : [];
}

function stash_state(string $key, mixed $value): void
{
    $_SESSION['_flash_state'][$key] = $value;
}

function consume_state(string $key, mixed $default = null): mixed
{
    if (!isset($_SESSION['_flash_state'][$key])) {
        return $default;
    }

    $value = $_SESSION['_flash_state'][$key];
    unset($_SESSION['_flash_state'][$key]);
    return $value;
}

function login_user_session(array $payload): void
{
    $_SESSION['auth'] = $payload;
}

function logout_user_session(): void
{
    unset($_SESSION['auth']);
}

function is_authenticated(): bool
{
    return !empty($_SESSION['auth']['company_api_key']);
}

function is_admin_session(): bool
{
    return !empty($_SESSION['auth']['is_admin']);
}

function require_authentication(): void
{
    if (!is_authenticated()) {
        flash('danger', 'Faça login para acessar essa área.');
        redirect('/login');
    }
}

function require_admin(): void
{
    require_authentication();
    if (!is_admin_session()) {
        flash('danger', 'Essa área é exclusiva para administradores do sistema.');
        redirect('/dashboard');
    }
}

function current_user_session(): array
{
    return $_SESSION['auth'] ?? [];
}

function current_company_api_key(): ?string
{
    return $_SESSION['auth']['company_api_key'] ?? null;
}

function current_user_id(): ?int
{
    $userId = $_SESSION['auth']['user_id'] ?? null;
    return is_numeric($userId) ? (int) $userId : null;
}

function current_api_headers(): array
{
    if (is_admin_session()) {
        return [];
    }

    $userId = current_user_id();
    if ($userId === null) {
        return [];
    }

    return ['X-USER-ID' => (string) $userId];
}

function guard_authenticated_api_exception(App\Api\ApiException $exception): void
{
    if ($exception->getStatusCode() === 401 && should_logout_from_api_exception($exception)) {
        logout_user_session();
        flash('danger', 'Sua sessão expirou ou a API key deixou de ser válida.');
        redirect('/login');
    }
}

function should_logout_from_api_exception(App\Api\ApiException $exception): bool
{
    $message = strtolower(trim($exception->getMessage()));
    if ($message === '') {
        return false;
    }

    return str_contains($message, 'api key da company')
        || str_contains($message, 'company api key')
        || str_contains($message, 'sessão expirou')
        || str_contains($message, 'session expired');
}

function public_api_base_url(): string
{
    $explicit = trim((string) getenv('SPRING_PUBLIC_BASE_URL'));
    if ($explicit !== '') {
        return rtrim($explicit, '/');
    }

    $configured = trim((string) (getenv('SPRING_API_BASE_URL') ?: 'http://localhost:8080'));
    $parts = parse_url($configured);
    if (!is_array($parts)) {
        return 'http://localhost:8080';
    }

    $host = (string) ($parts['host'] ?? 'localhost');
    $port = (int) ($parts['port'] ?? 8080);
    $scheme = (string) ($parts['scheme'] ?? 'http');

    if ($host === 'app' || $host === 'backend' || $host === 'spring') {
        $requestHost = (string) ($_SERVER['HTTP_HOST'] ?? 'localhost:8081');
        $hostname = preg_replace('/:\d+$/', '', $requestHost) ?: 'localhost';
        return sprintf('%s://%s:%d', request_scheme(), $hostname, $port);
    }

    return sprintf('%s://%s%s', $scheme, $host, $port > 0 ? ':' . $port : '');
}

function swagger_ui_url(): string
{
    return public_api_base_url() . '/swagger-ui/index.html';
}

function openapi_json_url(): string
{
    return public_api_base_url() . '/v3/api-docs';
}

function request_scheme(): string
{
    $https = $_SERVER['HTTPS'] ?? '';
    if ($https === 'on' || $https === '1') {
        return 'https';
    }

    return 'http';
}
