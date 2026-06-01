<?php
$datasets = $datasets ?? [];
$jobs = $jobs ?? [];
$models = $models ?? [];
$records = $records ?? [];
$form = $form ?? ['company_id' => ''];
$isAdmin = !empty($isAdmin);
$companyId = (string) ($form['company_id'] ?? '');
$selectedDatasetId = (string) ($selectedDatasetId ?? '');
$basePath = $isAdmin ? '/admin/ml/retrain' : '/ml/retrain';
$querySuffix = $isAdmin && $companyId !== '' ? '?company_id=' . urlencode($companyId) : '';
$uploadAction = $isAdmin ? '/admin/ml/retrain' : '/ml/retrain';
$trainAction = $isAdmin ? '/admin/ml/train' : '/ml/train';
$deleteDatasetAction = $isAdmin ? '/admin/ml/datasets/delete' : '/ml/datasets/delete';
$activateModelAction = $isAdmin ? '/admin/ml/models/activate' : '/ml/models/activate';
?>

<section class="row g-4">
    <div class="col-lg-4">
        <div class="panel-card p-4 h-100">
            <span class="hero-badge mb-3">Machine Learning</span>
            <h1 class="section-title h3 mb-3">Treinamento de modelo</h1>

            <?php if (!empty($error)): ?>
                <div class="alert alert-danger"><?= e((string) $error) ?></div>
            <?php endif; ?>

            <?php if (!empty($result)): ?>
                <div class="alert alert-success">
                    <strong>Base cadastrada.</strong><br>
                    Status: <?= e((string) ($result['status'] ?? 'DISPONIVEL')) ?> |
                    Lote: #<?= e((string) ($result['input_id'] ?? $result['inputId'] ?? '-')) ?> |
                    Registros: <?= e((string) ($result['nRegistrosTreino'] ?? '-')) ?>
                </div>
            <?php endif; ?>

            <?php if ($isAdmin): ?>
                <form method="get" action="/admin/ml/retrain" class="row g-3 mb-4">
                    <div class="col-12">
                        <label class="form-label" for="company_filter">Company ID</label>
                        <input class="form-control form-control-lg" id="company_filter" name="company_id" type="number" min="1" value="<?= e($companyId) ?>">
                    </div>
                    <div class="col-12 d-grid">
                        <button class="btn btn-outline-dark btn-lg" type="submit">Filtrar</button>
                    </div>
                </form>
            <?php endif; ?>

            <form method="post" action="<?= e($uploadAction) ?>" enctype="multipart/form-data" class="row g-3">
                <?php if ($isAdmin): ?>
                    <div class="col-12">
                        <label class="form-label" for="company_id">Company ID para upload</label>
                        <input class="form-control form-control-lg" id="company_id" name="company_id" type="number" min="1" required value="<?= e($companyId) ?>">
                    </div>
                <?php endif; ?>
                <div class="col-12">
                    <label class="form-label" for="file">Nova base histórica .xlsx</label>
                    <input class="form-control form-control-lg" id="file" name="file" type="file" accept=".xlsx" required>
                </div>
                <div class="col-12 d-grid">
                    <button class="btn btn-primary btn-lg" type="submit">Cadastrar base</button>
                </div>
            </form>
        </div>
    </div>

    <div class="col-lg-8">
        <div class="panel-card p-4 h-100">
            <div class="d-flex justify-content-between align-items-start flex-wrap gap-3 mb-3">
                <div>
                    <h2 class="section-title h4 mb-1">Bases de dados</h2>
                    <p class="text-secondary mb-0">Cada arquivo enviado fica disponível para compor treinamentos futuros.</p>
                </div>
                <span class="stat-chip"><?= e((string) count($datasets)) ?> bases</span>
            </div>

            <form method="post" action="<?= e($trainAction) ?>">
                <?php if ($isAdmin): ?>
                    <input type="hidden" name="company_id" value="<?= e($companyId) ?>">
                <?php endif; ?>
                <div class="table-responsive">
                    <table class="table align-middle">
                        <thead>
                            <tr>
                                <th></th>
                                <th>Base</th>
                                <?php if ($isAdmin): ?><th>Company</th><?php endif; ?>
                                <th>Registros</th>
                                <th>Descartadas</th>
                                <th>Ações</th>
                            </tr>
                        </thead>
                        <tbody>
                            <?php if (empty($datasets)): ?>
                                <tr>
                                    <td colspan="<?= $isAdmin ? '6' : '5' ?>" class="text-center text-secondary py-4">Nenhuma base cadastrada.</td>
                                </tr>
                            <?php else: ?>
                                <?php foreach ($datasets as $dataset): ?>
                                    <?php $inputId = (string) ($dataset['inputId'] ?? ''); ?>
                                    <tr>
                                        <td><input class="form-check-input" type="checkbox" name="input_ids[]" value="<?= e($inputId) ?>"></td>
                                        <td>
                                            <strong>#<?= e($inputId) ?></strong>
                                            <div class="small text-secondary"><?= e((string) ($dataset['descricao'] ?? 'Base XLSX')) ?></div>
                                        </td>
                                        <?php if ($isAdmin): ?>
                                            <td>#<?= e((string) ($dataset['companyId'] ?? '-')) ?> <?= e((string) ($dataset['companyName'] ?? '')) ?></td>
                                        <?php endif; ?>
                                        <td><?= e((string) ($dataset['nRegistrosTreino'] ?? '0')) ?></td>
                                        <td><?= e((string) ($dataset['linhasDescartadas'] ?? '0')) ?></td>
                                        <td class="d-flex gap-2 flex-wrap">
                                            <?php
                                            $previewUrl = $basePath . ($querySuffix !== '' ? $querySuffix . '&' : '?') . 'dataset_id=' . urlencode($inputId);
                                            $deleteFormId = 'delete-dataset-' . preg_replace('/[^a-zA-Z0-9_-]/', '', $inputId);
                                            ?>
                                            <a class="btn btn-sm btn-outline-dark" href="<?= e($previewUrl) ?>">Ver dados</a>
                                            <button class="btn btn-sm btn-outline-danger" type="submit" form="<?= e($deleteFormId) ?>">Excluir</button>
                                        </td>
                                    </tr>
                                <?php endforeach; ?>
                            <?php endif; ?>
                        </tbody>
                    </table>
                </div>
                <div class="d-flex justify-content-end">
                    <button class="btn btn-primary px-4" type="submit" <?= empty($datasets) ? 'disabled' : '' ?>>Treinar com selecionadas</button>
                </div>
            </form>
            <?php foreach ($datasets as $dataset): ?>
                <?php
                $inputId = (string) ($dataset['inputId'] ?? '');
                $deleteFormId = 'delete-dataset-' . preg_replace('/[^a-zA-Z0-9_-]/', '', $inputId);
                ?>
                <form id="<?= e($deleteFormId) ?>" method="post" action="<?= e($deleteDatasetAction) ?>" onsubmit="return confirm('Excluir esta base de dados?');">
                    <?php if ($isAdmin): ?><input type="hidden" name="company_id" value="<?= e($companyId) ?>"><?php endif; ?>
                    <input type="hidden" name="input_id" value="<?= e($inputId) ?>">
                </form>
            <?php endforeach; ?>
        </div>
    </div>
</section>

<?php if ($selectedDatasetId !== ''): ?>
    <section class="panel-card p-4 mt-4">
        <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-3">
            <div>
                <h2 class="section-title h4 mb-1">Dados da base #<?= e($selectedDatasetId) ?></h2>
                <p class="text-secondary mb-0">Prévia limitada aos primeiros 500 registros.</p>
            </div>
            <a class="btn btn-outline-dark" href="<?= e($basePath . $querySuffix) ?>">Fechar prévia</a>
        </div>
        <div class="table-responsive">
            <table class="table align-middle">
                <thead>
                    <tr>
                        <th>Origem</th>
                        <th>Destino</th>
                        <th>Peso</th>
                        <th>m³</th>
                        <th>Valor NF</th>
                        <th>Volumes</th>
                        <th>Via</th>
                        <th>Frete</th>
                        <th>Transit time</th>
                    </tr>
                </thead>
                <tbody>
                    <?php if (empty($records)): ?>
                        <tr><td colspan="9" class="text-center text-secondary py-4">Nenhum registro encontrado.</td></tr>
                    <?php else: ?>
                        <?php foreach ($records as $record): ?>
                            <tr>
                                <td><?= e((string) ($record['ufEmitenteNf'] ?? '-')) ?></td>
                                <td><?= e((string) ($record['ufDestinatarioNf'] ?? '-')) ?></td>
                                <td><?= e((string) ($record['pesoTotalBruto'] ?? '-')) ?></td>
                                <td><?= e((string) ($record['metroCubico'] ?? '-')) ?></td>
                                <td><?= e((string) ($record['valorNf'] ?? '-')) ?></td>
                                <td><?= e((string) ($record['volumeNf'] ?? '-')) ?></td>
                                <td><?= e((string) ($record['viaTransporte'] ?? '-')) ?></td>
                                <td><?= e((string) ($record['tipoFreteNf'] ?? '-')) ?></td>
                                <td><?= e((string) ($record['transitTimeDias'] ?? '-')) ?> dias</td>
                            </tr>
                        <?php endforeach; ?>
                    <?php endif; ?>
                </tbody>
            </table>
        </div>
    </section>
<?php endif; ?>

<section class="row g-4 mt-1">
    <div class="col-lg-7">
        <div class="panel-card p-4 h-100">
            <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-3">
                <div>
                    <h2 class="section-title h4 mb-1">Modelos treinados</h2>
                    <p class="text-secondary mb-0">Somente um modelo fica ativo por company.</p>
                </div>
                <span class="stat-chip"><?= e((string) count($models)) ?> modelos</span>
            </div>
            <div class="table-responsive">
                <table class="table align-middle">
                    <thead>
                        <tr>
                            <th>Modelo</th>
                            <?php if ($isAdmin): ?><th>Company</th><?php endif; ?>
                            <th>Status</th>
                            <th>Métricas</th>
                            <th>Ação</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php if (empty($models)): ?>
                            <tr><td colspan="<?= $isAdmin ? '5' : '4' ?>" class="text-center text-secondary py-4">Nenhum modelo treinado.</td></tr>
                        <?php else: ?>
                            <?php foreach ($models as $model): ?>
                                <tr>
                                    <td>
                                        <strong>#<?= e((string) ($model['id'] ?? '-')) ?></strong>
                                        <div class="small text-secondary"><?= e((string) ($model['artifactsId'] ?? '-')) ?></div>
                                        <div class="small text-secondary">Bases: <?= e((string) ($model['origemInputIds'] ?? $model['inputId'] ?? '-')) ?></div>
                                    </td>
                                    <?php if ($isAdmin): ?>
                                        <td>#<?= e((string) ($model['companyId'] ?? '-')) ?> <?= e((string) ($model['companyName'] ?? '')) ?></td>
                                    <?php endif; ?>
                                    <td>
                                        <?php if (!empty($model['ativo'])): ?>
                                            <span class="badge text-bg-success">ATIVO</span>
                                        <?php else: ?>
                                            <span class="badge text-bg-secondary">INATIVO</span>
                                        <?php endif; ?>
                                    </td>
                                    <td>
                                        MAE <?= e((string) ($model['maeKfold'] ?? '-')) ?><br>
                                        RMSE <?= e((string) ($model['rmseKfold'] ?? '-')) ?><br>
                                        R2 <?= e((string) ($model['r2Kfold'] ?? '-')) ?>
                                    </td>
                                    <td>
                                        <?php if (empty($model['ativo'])): ?>
                                            <form method="post" action="<?= e($activateModelAction) ?>">
                                                <?php if ($isAdmin): ?><input type="hidden" name="company_id" value="<?= e($companyId) ?>"><?php endif; ?>
                                                <input type="hidden" name="model_id" value="<?= e((string) ($model['id'] ?? '')) ?>">
                                                <button class="btn btn-sm btn-outline-dark" type="submit">Ativar</button>
                                            </form>
                                        <?php else: ?>
                                            <span class="text-secondary small">Em uso nas previsões</span>
                                        <?php endif; ?>
                                    </td>
                                </tr>
                            <?php endforeach; ?>
                        <?php endif; ?>
                    </tbody>
                </table>
            </div>
        </div>
    </div>

    <div class="col-lg-5">
        <div class="info-card p-4 h-100">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <h2 class="section-title h4 mb-0">Treinos recentes</h2>
                <a class="btn btn-sm btn-outline-dark" href="<?= e($isAdmin ? '/admin/ml/trainings' . $querySuffix : '/ml/trainings') ?>">Histórico</a>
            </div>
            <div class="table-responsive">
                <table class="table align-middle">
                    <thead>
                        <tr>
                            <th>Lote</th>
                            <th>Status</th>
                            <th>Registros</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php if (empty($jobs)): ?>
                            <tr><td colspan="3" class="text-center text-secondary py-4">Nenhum treino iniciado.</td></tr>
                        <?php else: ?>
                            <?php foreach (array_slice($jobs, 0, 6) as $job): ?>
                                <tr>
                                    <td>#<?= e((string) ($job['inputId'] ?? '-')) ?></td>
                                    <td><?= e((string) ($job['status'] ?? '-')) ?></td>
                                    <td><?= e((string) ($job['nRegistrosTreino'] ?? '0')) ?></td>
                                </tr>
                            <?php endforeach; ?>
                        <?php endif; ?>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
</section>
