<?php

declare(strict_types=1);

namespace App\Api;

final class ApiException extends \RuntimeException
{
    public function __construct(
        string $message,
        private readonly int $statusCode = 500,
        private readonly array $payload = []
    ) {
        parent::__construct($message, $statusCode);
    }

    public function getStatusCode(): int
    {
        return $this->statusCode;
    }

    public function getPayload(): array
    {
        return $this->payload;
    }
}

final class ApiClient
{
    public function __construct(private readonly string $baseUrl)
    {
    }

    public function getJson(string $path, ?string $apiKey = null, array $extraHeaders = []): array
    {
        return $this->request('GET', $path, null, [
            'Accept: application/json',
        ], $apiKey, $extraHeaders);
    }

    public function postJson(string $path, array $payload, ?string $apiKey = null, array $extraHeaders = []): array
    {
        $body = json_encode($payload, JSON_UNESCAPED_UNICODE);
        if ($body === false) {
            throw new ApiException('Falha ao serializar o payload da requisição.');
        }

        return $this->request('POST', $path, $body, [
            'Accept: application/json',
            'Content-Type: application/json',
        ], $apiKey, $extraHeaders);
    }

    public function postMultipart(string $path, array $files, array $fields = [], ?string $apiKey = null, array $extraHeaders = []): array
    {
        $boundary = '----TMA-' . bin2hex(random_bytes(12));
        $body = $this->buildMultipartBody($fields, $files, $boundary);

        return $this->request('POST', $path, $body, [
            'Accept: application/json',
            'Content-Type: multipart/form-data; boundary=' . $boundary,
        ], $apiKey, $extraHeaders);
    }

    public function patchJson(string $path, array $payload, ?string $apiKey = null, array $extraHeaders = []): array
    {
        $body = json_encode($payload, JSON_UNESCAPED_UNICODE);
        if ($body === false) {
            throw new ApiException('Falha ao serializar o payload da requisição.');
        }

        return $this->request('PATCH', $path, $body, [
            'Accept: application/json',
            'Content-Type: application/json',
        ], $apiKey, $extraHeaders);
    }

    public function putJson(string $path, array $payload, ?string $apiKey = null, array $extraHeaders = []): array
    {
        $body = json_encode($payload, JSON_UNESCAPED_UNICODE);
        if ($body === false) {
            throw new ApiException('Falha ao serializar o payload da requisição.');
        }

        return $this->request('PUT', $path, $body, [
            'Accept: application/json',
            'Content-Type: application/json',
        ], $apiKey, $extraHeaders);
    }

    public function delete(string $path, ?string $apiKey = null, array $extraHeaders = []): array
    {
        return $this->request('DELETE', $path, null, [
            'Accept: application/json',
        ], $apiKey, $extraHeaders);
    }

    private function request(string $method, string $path, ?string $body, array $headers, ?string $apiKey, array $extraHeaders = []): array
    {
        $requestHeaders = $headers;
        if ($apiKey !== null && $apiKey !== '') {
            $requestHeaders[] = 'X-API-KEY: ' . $apiKey;
        }
        foreach ($extraHeaders as $name => $value) {
            $requestHeaders[] = $name . ': ' . $value;
        }

        $context = stream_context_create([
            'http' => [
                'method' => $method,
                'header' => implode("\r\n", $requestHeaders),
                'content' => $body ?? '',
                'ignore_errors' => true,
                'timeout' => 60,
            ],
        ]);

        $responseBody = @file_get_contents($this->baseUrl . $path, false, $context);
        $responseHeaders = $http_response_header ?? [];
        $statusCode = $this->extractStatusCode($responseHeaders);
        $decoded = $this->decodeBody($responseBody);

        if ($statusCode < 200 || $statusCode >= 300) {
            throw new ApiException(
                $this->extractErrorMessage($decoded, $responseBody, $statusCode),
                $statusCode,
                $decoded
            );
        }

        return $decoded;
    }

    private function buildMultipartBody(array $fields, array $files, string $boundary): string
    {
        $lines = [];

        foreach ($fields as $name => $value) {
            $lines[] = '--' . $boundary;
            $lines[] = 'Content-Disposition: form-data; name="' . $name . '"';
            $lines[] = '';
            $lines[] = (string) $value;
        }

        foreach ($files as $fieldName => $file) {
            if (!is_array($file) || (int) ($file['error'] ?? UPLOAD_ERR_NO_FILE) !== UPLOAD_ERR_OK) {
                throw new ApiException('Selecione um arquivo válido antes de enviar.', 422);
            }

            $filename = (string) ($file['name'] ?? 'upload.xlsx');
            $mimeType = function_exists('mime_content_type')
                ? (mime_content_type((string) $file['tmp_name']) ?: 'application/octet-stream')
                : 'application/octet-stream';
            $content = file_get_contents((string) $file['tmp_name']);

            if ($content === false) {
                throw new ApiException('Não foi possível ler o arquivo selecionado.', 422);
            }

            $lines[] = '--' . $boundary;
            $lines[] = 'Content-Disposition: form-data; name="' . $fieldName . '"; filename="' . addslashes($filename) . '"';
            $lines[] = 'Content-Type: ' . $mimeType;
            $lines[] = '';
            $lines[] = $content;
        }

        $lines[] = '--' . $boundary . '--';
        $lines[] = '';

        return implode("\r\n", $lines);
    }

    private function extractStatusCode(array $headers): int
    {
        foreach ($headers as $header) {
            if (preg_match('/HTTP\/\S+\s+(\d{3})/', $header, $matches) === 1) {
                return (int) $matches[1];
            }
        }

        return 500;
    }

    private function decodeBody(string|false $responseBody): array
    {
        if ($responseBody === false || $responseBody === '') {
            return [];
        }

        $decoded = json_decode($responseBody, true);
        return is_array($decoded) ? $decoded : [];
    }

    private function extractErrorMessage(array $payload, string|false $responseBody, int $statusCode): string
    {
        foreach (['message', 'error'] as $key) {
            if (!empty($payload[$key]) && is_string($payload[$key])) {
                return $payload[$key];
            }
        }

        if (is_string($responseBody) && trim($responseBody) !== '') {
            return trim($responseBody);
        }

        return 'A API retornou um erro inesperado. Código HTTP: ' . $statusCode;
    }
}
