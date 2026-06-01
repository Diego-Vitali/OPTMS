<?php
$jobs = $jobs ?? [];
$form = $form ?? ['company_id' => ''];
$isAdmin = !empty($isAdmin);

function training_status_badge(string $status): string
{
    return match (strtoupper($status)) {
        'CONCLUIDO' => 'success',
        'FALHA' => 'danger',
        default => 'warning',
    };
}
?>

<section class="row g-4">
    <?php if ($isAdmin): ?>
        <div class="col-lg-4">
            <div class="panel-card p-4 h-100">
                <span class="hero-badge mb-3">Admin</span>
                <h1 class="section-title h3 mb-3">Filtrar treinamentos</h1>
                <form method="get" action="/admin/ml/trainings" class="row g-3">
                    <div class="col-12">
                        <label class="form-label" for="company_id">ID da empresa</label>
                        <input class="form-control form-control-lg" id="company_id" name="company_id" type="number" min="1" value="<?= e((string) ($form['company_id'] ?? '')) ?>">
                    </div>
                    <div class="col-12 d-grid">
                        <button class="btn btn-primary btn-lg" type="submit">Consultar</button>
                    </div>
                </form>
            </div>
        </div>
    <?php endif; ?>

    <div class="<?= $isAdmin ? 'col-lg-8' : 'col-12' ?>">
        <div class="panel-card p-4 h-100">
            <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-3">
                <div>
                    <span class="hero-badge mb-2">Previsão</span>
                    <h2 class="section-title h4 mb-1">Treinamentos realizados</h2>
                    <p class="text-secondary mb-0">Acompanhe os lotes em treinamento, concluídos e com falha.</p>
                </div>
                <div class="d-flex gap-2">
                    <a class="btn btn-outline-dark" href="<?= $isAdmin ? '/admin/ml/retrain' : '/ml/retrain' ?>">Novo treino</a>
                    <span class="stat-chip"><?= e((string) count($jobs)) ?> lotes</span>
                </div>
            </div>

            <?php if (!empty($error)): ?>
                <div class="alert alert-danger"><?= e((string) $error) ?></div>
            <?php endif; ?>

            <div class="table-responsive">
                <table class="table align-middle">
                    <thead>
                        <tr>
                            <th>Lote</th>
                            <?php if ($isAdmin): ?><th>Empresa</th><?php endif; ?>
                            <th>Status</th>
                            <th>Registros</th>
                            <th>Descartadas</th>
                            <th>Métricas</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php if (empty($jobs)): ?>
                            <tr>
                                <td colspan="<?= $isAdmin ? '6' : '5' ?>" class="text-center text-secondary py-4">Nenhum treinamento encontrado.</td>
                            </tr>
                        <?php else: ?>
                            <?php foreach ($jobs as $job): ?>
                                <?php $status = (string) ($job['status'] ?? 'TREINANDO'); ?>
                                <tr>
                                    <td>#<?= e((string) ($job['inputId'] ?? '-')) ?></td>
                                    <?php if ($isAdmin): ?>
                                        <td>#<?= e((string) ($job['companyId'] ?? '-')) ?> <?= e((string) ($job['companyName'] ?? '')) ?></td>
                                    <?php endif; ?>
                                    <td>
                                        <span class="badge text-bg-<?= e(training_status_badge($status)) ?>"><?= e($status) ?></span>
                                        <?php if (!empty($job['mensagemErro'])): ?>
                                            <div class="small text-danger mt-1"><?= e((string) $job['mensagemErro']) ?></div>
                                        <?php endif; ?>
                                    </td>
                                    <td><?= e((string) ($job['nRegistrosTreino'] ?? '0')) ?></td>
                                    <td><?= e((string) ($job['linhasDescartadas'] ?? '0')) ?></td>
                                    <td>
                                        MAE <?= e((string) ($job['maeKfold'] ?? '-')) ?><br>
                                        RMSE <?= e((string) ($job['rmseKfold'] ?? '-')) ?><br>
                                        R2 <?= e((string) ($job['r2Kfold'] ?? '-')) ?>
                                    </td>
                                </tr>
                            <?php endforeach; ?>
                        <?php endif; ?>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</section>
