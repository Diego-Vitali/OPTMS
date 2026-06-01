<?php $session = $session ?? []; ?>

<section class="hero-shell p-4 p-lg-5 mb-4">
    <span class="hero-badge mb-3">Painel</span>
    <div class="row g-4 align-items-end">
        <div class="col-lg-8">
            <h1 class="display-6 fw-bold mb-3"><?= e(app_name()) ?> na operação da <?= e($session['company_name'] ?? 'sua empresa') ?></h1>
            <p class="lead text-white-50 mb-0">
                <?= e(app_slogan()) ?>
            </p>
        </div>
        <div class="col-lg-4">
            <div class="info-card p-3">
                <div class="small text-uppercase text-secondary fw-semibold mb-2">Sessão atual</div>
                <div class="fw-semibold"><?= e($session['user_name'] ?? '') ?></div>
                <div class="text-secondary"><?= e($session['user_email'] ?? '') ?></div>
            </div>
        </div>
    </div>
</section>

<section class="row g-4">
    <div class="col-md-6 col-xl-4">
        <a class="quick-link" href="/usuarios">
            <div class="panel-card p-4 h-100">
                <div class="quick-link-icon mb-3">01</div>
                <h2 class="h5 section-title">Usuários internos</h2>
                <p class="text-secondary mb-0">Crie, edite e ative ou desative usuários da mesma empresa sem expor credenciais no formulário.</p>
            </div>
        </a>
    </div>
    <div class="col-md-6 col-xl-4">
        <a class="quick-link" href="/tabelas-frete">
            <div class="panel-card p-4 h-100">
                <div class="quick-link-icon mb-3">02</div>
                <h2 class="h5 section-title">Upload de frete</h2>
                <p class="text-secondary mb-0">Envie planilhas `.xlsx` e acompanhe as regras de cobrança cadastradas.</p>
            </div>
        </a>
    </div>
    <div class="col-md-6 col-xl-4">
        <a class="quick-link" href="/cotacoes">
            <div class="panel-card p-4 h-100">
                <div class="quick-link-icon mb-3">03</div>
                <h2 class="h5 section-title">Cotações</h2>
                <p class="text-secondary mb-0">Simule o valor do frete usando as tabelas ativas da sua empresa e veja o detalhamento por componente.</p>
            </div>
        </a>
    </div>
    <div class="col-md-6 col-xl-4">
        <a class="quick-link" href="/previsoes">
            <div class="panel-card p-4 h-100">
                <div class="quick-link-icon mb-3">04</div>
                <h2 class="h5 section-title">Previsões de entrega</h2>
                <p class="text-secondary mb-0">Consulte o prazo previsto com base nos dados do embarque.</p>
            </div>
        </a>
    </div>
    <div class="col-md-6 col-xl-4">
        <a class="quick-link" href="/ml/retrain">
            <div class="panel-card p-4 h-100">
                <div class="quick-link-icon mb-3">05</div>
                <h2 class="h5 section-title">Treinamento de previsões</h2>
                <p class="text-secondary mb-0">Faça upload da base histórica e acompanhe os indicadores do modelo de previsão.</p>
            </div>
        </a>
    </div>
    <div class="col-md-6 col-xl-4">
        <a class="quick-link" href="/apikeys">
            <div class="panel-card p-4 h-100">
                <div class="quick-link-icon mb-3">06</div>
                <h2 class="h5 section-title">Chaves externas</h2>
                <p class="text-secondary mb-0">A única área em que as chaves ficam visíveis para gestão de consumidores externos.</p>
            </div>
        </a>
    </div>
</section>
