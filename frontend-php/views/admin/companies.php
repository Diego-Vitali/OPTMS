<?php
$companies = $companies ?? [];
$form = $form ?? ['name' => '', 'social_name' => '', 'document' => ''];
?>

<section class="row g-4">
    <div class="col-lg-4">
        <div class="panel-card p-4 h-100">
            <span class="hero-badge mb-3">Admin</span>
            <h1 class="section-title h3 mb-3">Nova empresa</h1>
            <p class="text-secondary mb-4">Crie a empresa e depois use o ID dela nas demais operações administrativas.</p>

            <?php if (!empty($error)): ?>
                <div class="alert alert-danger"><?= e((string) $error) ?></div>
            <?php endif; ?>

            <form method="post" action="/admin/empresas" class="row g-3">
                <div class="col-12">
                    <label class="form-label" for="name">Nome</label>
                    <input class="form-control form-control-lg" id="name" name="name" type="text" required value="<?= e((string) ($form['name'] ?? '')) ?>">
                </div>
                <div class="col-12">
                    <label class="form-label" for="social_name">Razão social</label>
                    <input class="form-control form-control-lg" id="social_name" name="social_name" type="text" value="<?= e((string) ($form['social_name'] ?? '')) ?>">
                </div>
                <div class="col-12">
                    <label class="form-label" for="document">Documento</label>
                    <input class="form-control form-control-lg" id="document" name="document" type="text" value="<?= e((string) ($form['document'] ?? '')) ?>">
                </div>
                <div class="col-12 d-grid">
                    <button class="btn btn-primary btn-lg" type="submit">Criar empresa</button>
                </div>
            </form>
        </div>
    </div>

    <div class="col-lg-8">
        <div class="panel-card p-4 h-100">
            <div class="d-flex justify-content-between align-items-center mb-3">
                <div>
                    <span class="hero-badge mb-2">Admin</span>
                    <h2 class="section-title h4 mb-1">Empresas cadastradas</h2>
                    <p class="text-secondary mb-0">Gerencie status, edição e exclusão de empresas diretamente no painel.</p>
                </div>
                <span class="stat-chip"><?= e((string) count($companies)) ?> empresas</span>
            </div>

            <div class="table-responsive">
                <table class="table align-middle">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Nome</th>
                            <th>Razão social</th>
                            <th>Documento</th>
                            <th>Status</th>
                            <th class="text-end">Ações</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php if (empty($companies)): ?>
                            <tr>
                                <td colspan="6" class="text-center text-secondary py-4">Nenhuma empresa encontrada.</td>
                            </tr>
                        <?php else: ?>
                            <?php foreach ($companies as $company): ?>
                                <tr>
                                    <td><strong><?= e((string) ($company['id'] ?? '-')) ?></strong></td>
                                    <td><?= e((string) ($company['name'] ?? '-')) ?></td>
                                    <td><?= e((string) ($company['socialName'] ?? '-')) ?></td>
                                    <td><?= e((string) ($company['document'] ?? '-')) ?></td>
                                    <td>
                                        <?php if (!empty($company['active'])): ?>
                                            <span class="badge text-bg-success">Ativa</span>
                                        <?php else: ?>
                                            <span class="badge text-bg-secondary">Inativa</span>
                                        <?php endif; ?>
                                    </td>
                                    <td class="text-end">
                                        <div class="d-flex justify-content-end gap-2 flex-wrap">
                                            <a class="btn btn-sm btn-outline-dark" href="/admin/empresas/editar?id=<?= e((string) ($company['id'] ?? '')) ?>">Editar</a>
                                            <form method="post" action="/admin/empresas/acao">
                                                <input type="hidden" name="id" value="<?= e((string) ($company['id'] ?? '')) ?>">
                                                <input type="hidden" name="action" value="<?= !empty($company['active']) ? 'desativar' : 'ativar' ?>">
                                                <button class="btn btn-sm <?= !empty($company['active']) ? 'btn-outline-warning' : 'btn-outline-success' ?>" type="submit">
                                                    <?= !empty($company['active']) ? 'Desativar' : 'Ativar' ?>
                                                </button>
                                            </form>
                                            <form method="post" action="/admin/empresas/acao" onsubmit="return confirm('Excluir esta empresa e todos os dados relacionados?');">
                                                <input type="hidden" name="id" value="<?= e((string) ($company['id'] ?? '')) ?>">
                                                <input type="hidden" name="action" value="excluir">
                                                <button class="btn btn-sm btn-outline-danger" type="submit">Excluir</button>
                                            </form>
                                        </div>
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
