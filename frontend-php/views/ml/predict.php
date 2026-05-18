<?php
$form = $form ?? [];
$result = $result ?? [];
?>

<section class="row g-4">
    <div class="col-lg-7">
        <div class="panel-card p-4 p-lg-5">
            <span class="hero-badge mb-3">Machine Learning</span>
            <h1 class="section-title h2 mb-3">Previsão de transit time</h1>
            <p class="text-secondary mb-4">
                Preencha os dados do embarque para estimar o prazo de entrega com o modelo treinado.
            </p>

            <?php if (!empty($error)): ?>
                <div class="alert alert-danger"><?= e((string) $error) ?></div>
            <?php endif; ?>

            <?php if (!empty($result['error'])): ?>
                <div class="alert alert-warning"><?= e((string) $result['error']) ?></div>
            <?php endif; ?>

            <form method="post" action="/previsoes" class="row g-3">
                <div class="col-md-6">
                    <label class="form-label" for="peso_total_bruto">Peso total bruto</label>
                    <input class="form-control form-control-lg" id="peso_total_bruto" name="peso_total_bruto" type="number" step="0.01" min="0.01" required value="<?= e($form['peso_total_bruto'] ?? '') ?>">
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="metro_cubico">Metro cúbico</label>
                    <input class="form-control form-control-lg" id="metro_cubico" name="metro_cubico" type="number" step="0.01" min="0.01" required value="<?= e($form['metro_cubico'] ?? '') ?>">
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="valor_nf">Valor NF</label>
                    <input class="form-control form-control-lg" id="valor_nf" name="valor_nf" type="number" step="0.01" min="0" required value="<?= e($form['valor_nf'] ?? '') ?>">
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="volume_nf">Volume NF</label>
                    <input class="form-control form-control-lg" id="volume_nf" name="volume_nf" type="number" step="1" min="1" required value="<?= e($form['volume_nf'] ?? '') ?>">
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="tipo_frete_nf">Tipo de frete NF</label>
                    <select class="form-select form-select-lg" id="tipo_frete_nf" name="tipo_frete_nf" required>
                        <?php foreach (['CIF', 'FOB'] as $option): ?>
                            <option value="<?= e($option) ?>" <?= (($form['tipo_frete_nf'] ?? '') === $option) ? 'selected' : '' ?>><?= e($option) ?></option>
                        <?php endforeach; ?>
                    </select>
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="via_transporte">Via de transporte</label>
                    <select class="form-select form-select-lg" id="via_transporte" name="via_transporte" required>
                        <?php foreach (['Rodoviário', 'Aéreo', 'Marítimo', 'Ferroviário', 'Cabotagem'] as $option): ?>
                            <option value="<?= e($option) ?>" <?= (($form['via_transporte'] ?? '') === $option) ? 'selected' : '' ?>><?= e($option) ?></option>
                        <?php endforeach; ?>
                    </select>
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="uf_emitente_nf">UF emitente</label>
                    <input class="form-control form-control-lg" id="uf_emitente_nf" name="uf_emitente_nf" type="text" maxlength="2" required value="<?= e($form['uf_emitente_nf'] ?? '') ?>">
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="uf_destinatario_nf">UF destinatário</label>
                    <input class="form-control form-control-lg" id="uf_destinatario_nf" name="uf_destinatario_nf" type="text" maxlength="2" required value="<?= e($form['uf_destinatario_nf'] ?? '') ?>">
                </div>
                <div class="col-12 d-flex gap-3">
                    <button class="btn btn-primary btn-lg px-4" type="submit">Prever prazo</button>
                    <a class="btn btn-outline-dark btn-lg px-4" href="<?= !empty($isAdmin) ? '/admin' : '/dashboard' ?>">Voltar</a>
                </div>
            </form>
        </div>
    </div>
    <div class="col-lg-5">
        <div class="info-card p-4 h-100">
            <h2 class="section-title h5 mb-3">Resultado previsto</h2>
            <?php if (!empty($result) && empty($result['error']) && isset($result['predictedTransitTime'])): ?>
                <div class="prediction-card text-center">
                    <div class="small text-uppercase text-secondary fw-semibold mb-2">Transit time estimado</div>
                    <div class="display-5 fw-bold text-navy"><?= e(number_format((float) $result['predictedTransitTime'], 2, ',', '.')) ?></div>
                    <p class="text-secondary mb-0">dias</p>
                </div>
            <?php else: ?>
                <p class="list-spec mb-0">O modelo retornará a estimativa em dias com base nas características logísticas e fiscais do embarque.</p>
            <?php endif; ?>
        </div>
    </div>
</section>
