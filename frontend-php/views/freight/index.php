<?php
$tables = $tables ?? [];
$form = $form ?? ['company_id' => ''];
$isAdmin = !empty($isAdmin);
?>

<section class="row g-4">
    <?php if ($isAdmin): ?>
        <div class="col-lg-4">
            <div class="panel-card p-4 h-100">
                <span class="hero-badge mb-3">Admin</span>
                <h1 class="section-title h3 mb-3">Filtrar tabelas</h1>
                <p class="text-secondary mb-4">Informe o ID da empresa para filtrar ou deixe em branco para listar todas.</p>

                <?php if (!empty($error)): ?>
                    <div class="alert alert-danger"><?= e((string) $error) ?></div>
                <?php endif; ?>

                <form method="get" action="/admin/tabelas-frete" class="row g-3">
                    <div class="col-12">
                        <label class="form-label" for="company_id">ID da empresa</label>
                        <input class="form-control form-control-lg" id="company_id" name="company_id" type="number" min="1" value="<?= e((string) ($form['company_id'] ?? '')) ?>">
                    </div>
                    <div class="col-12 d-grid">
                        <button class="btn btn-primary btn-lg" type="submit">Consultar tabelas</button>
                    </div>
                </form>
            </div>
        </div>
    <?php endif; ?>

    <div class="<?= $isAdmin ? 'col-lg-8' : 'col-12' ?>">
        <div class="panel-card p-4 h-100">
            <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-3">
                <div>
                    <span class="hero-badge mb-2">Tabelas de frete</span>
                    <h2 class="section-title h4 mb-1">Tabelas cadastradas</h2>
                    <p class="text-secondary mb-0"><?= $isAdmin ? 'Gerencie o status das tabelas com escopo master.' : 'Visualize e ative ou desative as tabelas da sua empresa.' ?></p>
                </div>
                <div class="d-flex gap-2">
                    <a class="btn btn-outline-dark" href="<?= $isAdmin ? '/admin/tabelas-frete/upload' : '/tabelas-frete/upload' ?>">Nova tabela</a>
                    <span class="stat-chip"><?= e((string) count($tables)) ?> registros</span>
                </div>
            </div>

            <?php if (!$isAdmin && !empty($error)): ?>
                <div class="alert alert-danger"><?= e((string) $error) ?></div>
            <?php endif; ?>

            <div class="table-responsive">
                <table class="table align-middle">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <?php if ($isAdmin): ?><th>Empresa</th><?php endif; ?>
                            <th>Nome</th>
                            <th>UF origem</th>
                            <th>Status</th>
                            <th class="text-end">Ações</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php if (empty($tables)): ?>
                            <tr>
                                <td colspan="<?= $isAdmin ? '6' : '5' ?>" class="text-center text-secondary py-4">Nenhuma tabela encontrada.</td>
                            </tr>
                        <?php else: ?>
                            <?php foreach ($tables as $table): ?>
                                <tr>
                                    <td><?= e((string) ($table['id'] ?? '-')) ?></td>
                                    <?php if ($isAdmin): ?><td><?= e((string) ($table['companyId'] ?? '-')) ?></td><?php endif; ?>
                                    <td><?= e((string) ($table['nome'] ?? '-')) ?></td>
                                    <td><?= e((string) ($table['ufOrigem'] ?? '-')) ?></td>
                                    <td>
                                        <?php if (!empty($table['ativa'])): ?>
                                            <span class="badge text-bg-success">Ativa</span>
                                        <?php else: ?>
                                            <span class="badge text-bg-secondary">Inativa</span>
                                        <?php endif; ?>
                                    </td>
                                    <td class="text-end">
                                        <form method="post" action="<?= $isAdmin ? '/admin/tabelas-frete/acao' : '/tabelas-frete/acao' ?>" class="d-inline-flex">
                                            <input type="hidden" name="id" value="<?= e((string) ($table['id'] ?? '')) ?>">
                                            <?php if ($isAdmin): ?>
                                                <input type="hidden" name="company_id" value="<?= e((string) ($form['company_id'] ?? '')) ?>">
                                            <?php endif; ?>
                                            <input type="hidden" name="action" value="<?= !empty($table['ativa']) ? 'desativar' : 'ativar' ?>">
                                            <button class="btn btn-sm <?= !empty($table['ativa']) ? 'btn-outline-warning' : 'btn-outline-success' ?>" type="submit">
                                                <?= !empty($table['ativa']) ? 'Desativar' : 'Ativar' ?>
                                            </button>
                                        </form>
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
