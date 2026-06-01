<?php $session = $session ?? []; ?>

<section class="hero-shell p-4 p-lg-5 mb-4">
    <span class="hero-badge mb-3">Admin Master</span>
    <div class="row g-4 align-items-end">
        <div class="col-lg-8">
            <h1 class="display-6 fw-bold mb-3">Painel administrativo do sistema</h1>
            <p class="lead text-white-50 mb-0">
                <?= e(app_slogan()) ?>
            </p>
        </div>
        <div class="col-lg-4">
            <div class="info-card p-3">
                <div class="small text-uppercase text-secondary fw-semibold mb-2">Sessão atual</div>
                <div class="fw-semibold"><?= e($session['user_name'] ?? 'Administrador do sistema') ?></div>
                <div class="text-secondary"><?= e($session['user_email'] ?? 'admin') ?></div>
                <div class="text-secondary small mt-2">Escopo: acesso master</div>
            </div>
        </div>
    </div>
</section>

<section class="row g-4">
    <div class="col-md-6 col-xl-4">
        <a class="quick-link" href="/admin/empresas">
            <div class="panel-card p-4 h-100">
                <div class="quick-link-icon mb-3">01</div>
                <h2 class="h5 section-title">Empresas</h2>
                <p class="text-secondary mb-0">Visualize todas as companies cadastradas e seus respectivos IDs para operar com precisão.</p>
            </div>
        </a>
    </div>
    <div class="col-md-6 col-xl-4">
        <a class="quick-link" href="/admin/usuarios">
            <div class="panel-card p-4 h-100">
                <div class="quick-link-icon mb-3">02</div>
                <h2 class="h5 section-title">Usuários</h2>
                <p class="text-secondary mb-0">Crie e mantenha usuários em qualquer empresa informando o ID da company no formulário.</p>
            </div>
        </a>
    </div>
    <div class="col-md-6 col-xl-4">
        <a class="quick-link" href="/admin/tabelas-frete">
            <div class="panel-card p-4 h-100">
                <div class="quick-link-icon mb-3">03</div>
                <h2 class="h5 section-title">Tabelas de frete</h2>
                <p class="text-secondary mb-0">Faça upload de tabelas de frete para qualquer empresa cadastrada.</p>
            </div>
        </a>
    </div>
    <div class="col-md-6 col-xl-4">
        <a class="quick-link" href="/admin/cotacoes">
            <div class="panel-card p-4 h-100">
                <div class="quick-link-icon mb-3">04</div>
                <h2 class="h5 section-title">Cotações</h2>
                <p class="text-secondary mb-0">Calcule cotações para qualquer empresa usando o ID da company e as tabelas ativas dela.</p>
            </div>
        </a>
    </div>
    <div class="col-md-6 col-xl-4">
        <a class="quick-link" href="/previsoes">
            <div class="panel-card p-4 h-100">
                <div class="quick-link-icon mb-3">05</div>
                <h2 class="h5 section-title">Previsões ML</h2>
                <p class="text-secondary mb-0">Consulte o transit time previsto pelo modelo em um fluxo rápido e centralizado.</p>
            </div>
        </a>
    </div>
    <div class="col-md-6 col-xl-4">
        <a class="quick-link" href="/admin/apikeys">
            <div class="panel-card p-4 h-100">
                <div class="quick-link-icon mb-3">06</div>
                <h2 class="h5 section-title">API keys externas</h2>
                <p class="text-secondary mb-0">Gere credenciais de consumo externo para a empresa escolhida pelo ID.</p>
            </div>
        </a>
    </div>
    <div class="col-md-6 col-xl-4">
        <a class="quick-link" href="/admin/ml/retrain">
            <div class="panel-card p-4 h-100">
                <div class="quick-link-icon mb-3">07</div>
                <h2 class="h5 section-title">Treinamento ML</h2>
                <p class="text-secondary mb-0">Treine e ative modelos vinculados à Company informada pelo ID.</p>
            </div>
        </a>
    </div>
    <div class="col-md-6 col-xl-4">
        <a class="quick-link" href="/admin/ml/trainings">
            <div class="panel-card p-4 h-100">
                <div class="quick-link-icon mb-3">08</div>
                <h2 class="h5 section-title">Histórico ML</h2>
                <p class="text-secondary mb-0">Acompanhe treinamentos em execução, concluídos e com falha.</p>
            </div>
        </a>
    </div>
</section>
