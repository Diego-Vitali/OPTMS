<?php
$apiKeys = $apiKeys ?? [];
$actionPath = !empty($isAdmin) ? '/admin/apikeys/acao' : '/apikeys/acao';
?>

<section class="row g-4">
    <div class="col-lg-4">
        <div class="panel-card p-4 h-100">
            <span class="hero-badge mb-3">Consumidores externos</span>
            <h1 class="section-title h3 mb-3">Nova API key</h1>
            <p class="text-secondary">
                Crie uma credencial para outros sistemas consumirem as APIs do Spring Boot<?= !empty($isAdmin) ? ' da empresa indicada pelo ID' : ' dessa company' ?>.
            </p>

            <?php if (!empty($error)): ?>
                <div class="alert alert-danger"><?= e((string) $error) ?></div>
            <?php endif; ?>

            <?php if (!empty($success)): ?>
                <div class="alert alert-success"><?= e((string) $success) ?></div>
            <?php endif; ?>

            <form method="post" action="<?= !empty($isAdmin) ? '/admin/apikeys' : '/apikeys' ?>" class="row g-3">
                <?php if (!empty($isAdmin)): ?>
                    <div class="col-12">
                        <label class="form-label" for="company_id">ID da empresa</label>
                        <input class="form-control form-control-lg" id="company_id" name="company_id" type="number" min="1" required value="<?= e($form['company_id'] ?? '') ?>">
                    </div>
                <?php endif; ?>
                <div class="col-12">
                    <label class="form-label" for="custom_name">Nome de identificação</label>
                    <input class="form-control form-control-lg" id="custom_name" name="custom_name" type="text" required value="<?= e($form['custom_name'] ?? '') ?>">
                </div>
                <div class="col-12 d-grid">
                    <button class="btn btn-primary btn-lg" type="submit">Gerar nova key</button>
                </div>
            </form>
        </div>
    </div>
    <div class="col-lg-8">
        <div class="panel-card p-4 mb-4">
            <div class="d-flex justify-content-between align-items-start flex-wrap gap-3">
                <div>
                    <h2 class="section-title h4 mb-1">Documentação Swagger</h2>
                    <p class="text-secondary mb-0">Use estes links para abrir a documentação da API e copiar rapidamente os endereços de integração.</p>
                </div>
                <a class="btn btn-outline-dark" href="<?= e(swagger_ui_url()) ?>" target="_blank" rel="noreferrer">Abrir Swagger</a>
            </div>

            <div class="row g-3 mt-1">
                <div class="col-12">
                    <label class="form-label" for="swagger_ui_url">Swagger UI</label>
                    <div class="input-group">
                        <input class="form-control" id="swagger_ui_url" type="text" readonly value="<?= e(swagger_ui_url()) ?>">
                        <button class="btn btn-outline-dark" type="button" data-copy-text="<?= e(swagger_ui_url()) ?>">Copiar</button>
                    </div>
                </div>
                <div class="col-12">
                    <label class="form-label" for="openapi_json_url">OpenAPI JSON</label>
                    <div class="input-group">
                        <input class="form-control" id="openapi_json_url" type="text" readonly value="<?= e(openapi_json_url()) ?>">
                        <button class="btn btn-outline-dark" type="button" data-copy-text="<?= e(openapi_json_url()) ?>">Copiar</button>
                    </div>
                </div>
            </div>
        </div>

        <div class="panel-card p-4 h-100">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <div>
                    <h2 class="section-title h4 mb-1">Chaves cadastradas</h2>
                    <p class="text-secondary mb-0"><?= !empty($isAdmin) ? 'Listagem conforme o ID informado.' : 'Listagem atual da company autenticada.' ?></p>
                </div>
                <span class="stat-chip"><?= e((string) count($apiKeys)) ?> registros</span>
            </div>

            <div class="table-responsive">
                <table class="table align-middle">
                    <thead>
                            <tr>
                                <th>ID</th>
                                <?php if (!empty($isAdmin)): ?><th>Empresa</th><?php endif; ?>
                                <th>Nome</th>
                                <th>API key</th>
                                <th>Ações</th>
                                <th>Status</th>
                            </tr>
                    </thead>
                    <tbody>
                        <?php if (empty($apiKeys)): ?>
                            <tr>
                                <td colspan="<?= !empty($isAdmin) ? '6' : '5' ?>" class="text-secondary text-center py-4">Nenhuma API key externa cadastrada até o momento.</td>
                            </tr>
                        <?php else: ?>
                            <?php foreach ($apiKeys as $apiKeyItem): ?>
                                <tr>
                                    <td><?= e((string) ($apiKeyItem['id'] ?? '-')) ?></td>
                                    <?php if (!empty($isAdmin)): ?><td><?= e((string) ($apiKeyItem['companyId'] ?? '-')) ?></td><?php endif; ?>
                                    <td><?= e((string) ($apiKeyItem['customName'] ?? '-')) ?></td>
                                    <td><code><?= e((string) ($apiKeyItem['apikey'] ?? '-')) ?></code></td>
                                    <td>
                                        <div class="d-flex gap-2 flex-wrap">
                                            <button class="btn btn-sm btn-outline-dark" type="button" data-copy-text="<?= e((string) ($apiKeyItem['apikey'] ?? '')) ?>">Copiar key</button>
                                            <form method="post" action="<?= e($actionPath) ?>">
                                                <input type="hidden" name="id" value="<?= e((string) ($apiKeyItem['id'] ?? '')) ?>">
                                                <?php if (!empty($isAdmin)): ?>
                                                    <input type="hidden" name="company_id" value="<?= e((string) ($form['company_id'] ?? '')) ?>">
                                                <?php endif; ?>
                                                <input type="hidden" name="action" value="<?= !empty($apiKeyItem['active']) ? 'desativar' : 'ativar' ?>">
                                                <button class="btn btn-sm <?= !empty($apiKeyItem['active']) ? 'btn-outline-warning' : 'btn-outline-success' ?>" type="submit">
                                                    <?= !empty($apiKeyItem['active']) ? 'Desativar' : 'Ativar' ?>
                                                </button>
                                            </form>
                                            <?php if (!empty($isAdmin)): ?>
                                                <form method="post" action="/admin/apikeys/acao" onsubmit="return confirm('Excluir esta API key externa?');">
                                                    <input type="hidden" name="id" value="<?= e((string) ($apiKeyItem['id'] ?? '')) ?>">
                                                    <input type="hidden" name="company_id" value="<?= e((string) ($form['company_id'] ?? '')) ?>">
                                                    <input type="hidden" name="action" value="excluir">
                                                    <button class="btn btn-sm btn-outline-danger" type="submit">Excluir</button>
                                                </form>
                                            <?php endif; ?>
                                        </div>
                                    </td>
                                    <td>
                                        <?php if (!empty($apiKeyItem['active'])): ?>
                                            <span class="badge text-bg-success">Ativa</span>
                                        <?php else: ?>
                                            <span class="badge text-bg-secondary">Inativa</span>
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
</section>
