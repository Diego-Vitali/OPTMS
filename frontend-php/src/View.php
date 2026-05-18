<?php

declare(strict_types=1);

namespace App\View;

final class View
{
    public static function render(string $view, array $params = []): void
    {
        $viewPath = dirname(__DIR__) . '/views/' . $view . '.php';
        if (!is_file($viewPath)) {
            http_response_code(500);
            echo 'View não encontrada: ' . htmlspecialchars($view, ENT_QUOTES, 'UTF-8');
            return;
        }

        extract($params, EXTR_SKIP);

        ob_start();
        require $viewPath;
        $content = (string) ob_get_clean();

        $title = $params['title'] ?? app_name();
        $flashes = consume_flashes();
        require dirname(__DIR__) . '/views/layout.php';
    }
}
