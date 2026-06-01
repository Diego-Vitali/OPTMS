<?php
$form = $form ?? [];
$result = $result ?? [];
$predictedDays = $result['tma_estimado_dias'] ?? null;
$slaInterval = $result['intervalo_sla_dias'] ?? [];
$risk = $result['risco'] ?? null;
$factors = $result['top_fatores_explicacao'] ?? [];
$ufs = brazilian_ufs();
?>

<section class="row g-4">
    <div class="col-lg-7">
        <div class="panel-card p-4 p-lg-5">
            <span class="hero-badge mb-3">Previsão</span>
            <h1 class="section-title h2 mb-3">Previsão de prazo</h1>
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
                    <select class="form-select form-select-lg" id="uf_emitente_nf" name="uf_emitente_nf" required>
                        <option value="">Selecione</option>
                        <?php foreach ($ufs as $uf => $stateName): ?>
                            <option value="<?= e($uf) ?>" <?= normalize_brazilian_uf($form['uf_emitente_nf'] ?? '') === $uf ? 'selected' : '' ?>>
                                <?= e($uf . ' - ' . $stateName) ?>
                            </option>
                        <?php endforeach; ?>
                    </select>
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="uf_destinatario_nf">UF destinatário</label>
                    <select class="form-select form-select-lg" id="uf_destinatario_nf" name="uf_destinatario_nf" required>
                        <option value="">Selecione</option>
                        <?php foreach ($ufs as $uf => $stateName): ?>
                            <option value="<?= e($uf) ?>" <?= normalize_brazilian_uf($form['uf_destinatario_nf'] ?? '') === $uf ? 'selected' : '' ?>>
                                <?= e($uf . ' - ' . $stateName) ?>
                            </option>
                        <?php endforeach; ?>
                    </select>
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
            <?php if (!empty($result) && empty($result['error']) && $predictedDays !== null): ?>
                <div class="prediction-card">
                    <div class="text-center mb-4">
                        <div class="small text-uppercase text-secondary fw-semibold mb-2">Transit time estimado</div>
                        <div class="display-5 fw-bold text-navy"><?= e(number_format((float) $predictedDays, 1, ',', '.')) ?></div>
                        <p class="text-secondary mb-0">dias</p>
                    </div>

                    <div class="prediction-metrics">
                        <?php if (is_array($slaInterval) && count($slaInterval) >= 2): ?>
                            <div class="prediction-metric">
                                <span>Intervalo estimado</span>
                                <strong><?= e((string) $slaInterval[0]) ?>-<?= e((string) $slaInterval[1]) ?> dias</strong>
                            </div>
                        <?php endif; ?>

                        <?php if (!empty($risk)): ?>
                            <div class="prediction-metric">
                                <span>Risco</span>
                                <strong><?= e((string) $risk) ?></strong>
                            </div>
                        <?php endif; ?>

                    </div>

                    <?php if (is_array($factors) && !empty($factors)): ?>
                        <div class="prediction-factors mt-4">
                            <div class="small text-uppercase text-secondary fw-semibold mb-2">Fatores de impacto</div>
                            <?php foreach (array_slice($factors, 0, 3) as $factor): ?>
                                <?php
                                $variable = is_array($factor) ? ($factor['variavel'] ?? '-') : '-';
                                $impact = is_array($factor) ? ($factor['impacto_dias'] ?? null) : null;
                                ?>
                                <div class="prediction-factor">
                                    <span><?= e((string) $variable) ?></span>
                                    <?php if ($impact !== null): ?>
                                        <strong><?= e(number_format((float) $impact, 2, ',', '.')) ?> dias</strong>
                                    <?php endif; ?>
                                </div>
                            <?php endforeach; ?>
                        </div>
                    <?php endif; ?>

                </div>
            <?php else: ?>
                <p class="list-spec mb-0">O modelo retornará a estimativa em dias com base nas características logísticas e fiscais do embarque.</p>
            <?php endif; ?>
        </div>
    </div>
</section>
