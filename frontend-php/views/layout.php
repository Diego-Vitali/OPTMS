<!doctype html>
<html lang="pt-BR">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><?= e((string) $title) ?> | <?= e(app_name()) ?></title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="<?= e(asset_path('app.css')) ?>" rel="stylesheet">
</head>
<body>
    <?php if (is_authenticated()): ?>
        <nav class="navbar navbar-expand-lg navbar-dark app-navbar mb-4">
            <div class="container py-1">
                <a class="navbar-brand app-brand-wrap" href="<?= is_admin_session() ? '/admin' : '/dashboard' ?>">
                    <span class="app-brand"><?= e(app_name()) ?></span>
                </a>
                <button class="navbar-toggler border-0 shadow-none" type="button" data-bs-toggle="collapse" data-bs-target="#mainMenu" aria-controls="mainMenu" aria-expanded="false" aria-label="Abrir navegação">
                    <span class="navbar-toggler-icon"></span>
                </button>
                <div class="collapse navbar-collapse" id="mainMenu">
                    <ul class="navbar-nav ms-auto align-items-lg-center gap-lg-2">
                        <?php if (is_admin_session()): ?>
                            <li class="nav-item"><a class="nav-link" href="/admin">Admin</a></li>
                            <li class="nav-item"><a class="nav-link" href="/admin/empresas">Empresas</a></li>
                            <li class="nav-item"><a class="nav-link" href="/admin/usuarios">Usuários</a></li>
                            <li class="nav-item"><a class="nav-link" href="/admin/tabelas-frete">Tabelas</a></li>
                            <li class="nav-item"><a class="nav-link" href="/admin/cotacoes">Cotações</a></li>
                            <li class="nav-item"><a class="nav-link" href="/previsoes">Previsões</a></li>
                            <li class="nav-item"><a class="nav-link" href="/admin/apikeys">API Keys</a></li>
                            <li class="nav-item"><a class="nav-link" href="/admin/ml/retrain">Treinamento</a></li>
                        <?php else: ?>
                            <li class="nav-item"><a class="nav-link" href="/dashboard">Painel</a></li>
                            <li class="nav-item"><a class="nav-link" href="/usuarios">Usuários</a></li>
                            <li class="nav-item"><a class="nav-link" href="/tabelas-frete">Tabela de Frete</a></li>
                            <li class="nav-item"><a class="nav-link" href="/cotacoes">Cotações</a></li>
                            <li class="nav-item"><a class="nav-link" href="/previsoes">Previsões</a></li>
                            <li class="nav-item"><a class="nav-link" href="/ml/retrain">Treinamento</a></li>
                            <li class="nav-item"><a class="nav-link" href="/apikeys">API Keys</a></li>
                        <?php endif; ?>
                        <li class="nav-item"><a class="nav-link text-warning" href="/logout">Sair</a></li>
                    </ul>
                </div>
            </div>
        </nav>
    <?php endif; ?>

    <main class="container pb-5">
        <?php foreach ($flashes as $flashMessage): ?>
            <div class="alert alert-<?= e($flashMessage['type'] ?? 'info') ?> shadow-sm mb-3" role="alert">
                <?= e($flashMessage['message'] ?? '') ?>
            </div>
        <?php endforeach; ?>

        <?= $content ?>
    </main>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        document.addEventListener('click', function (event) {
            const button = event.target.closest('[data-copy-text]');
            if (!button) {
                return;
            }

            const original = button.textContent;
            navigator.clipboard.writeText(button.dataset.copyText || '').then(function () {
                button.textContent = 'Copiado';
                setTimeout(function () {
                    button.textContent = original;
                }, 1500);
            });
        });
    </script>
</body>
</html>
