<?php
$users = $users ?? [];
$form = $form ?? ['name' => '', 'email' => '', 'company_id' => ''];
$isAdmin = !empty($isAdmin);
?>

<section class="row g-4">
    <div class="col-lg-4">
        <div class="panel-card p-4 h-100">
            <span class="hero-badge mb-3"><?= $isAdmin ? 'Admin' : 'Interno' ?></span>
            <h1 class="section-title h3 mb-3">Novo usuário</h1>
            <p class="text-secondary">
                <?= $isAdmin
                    ? 'Crie usuários para qualquer empresa informando o ID da empresa.'
                    : 'Somente usuários já autenticados da mesma empresa podem criar outros usuários.' ?>
            </p>

            <?php if (!empty($error)): ?>
                <div class="alert alert-danger"><?= e((string) $error) ?></div>
            <?php endif; ?>

            <form method="post" action="<?= $isAdmin ? '/admin/usuarios' : '/usuarios' ?>" class="row g-3">
                <?php if ($isAdmin): ?>
                    <div class="col-12">
                        <label class="form-label" for="company_id">ID da empresa</label>
                        <input class="form-control form-control-lg" id="company_id" name="company_id" type="number" min="1" required value="<?= e($form['company_id'] ?? '') ?>">
                    </div>
                <?php endif; ?>
                <div class="col-12">
                    <label class="form-label" for="name">Nome</label>
                    <input class="form-control form-control-lg" id="name" name="name" type="text" required value="<?= e($form['name'] ?? '') ?>">
                </div>
                <div class="col-12">
                    <label class="form-label" for="email">E-mail</label>
                    <input class="form-control form-control-lg" id="email" name="email" type="email" required value="<?= e($form['email'] ?? '') ?>">
                </div>
                <div class="col-12">
                    <label class="form-label" for="password">Senha</label>
                    <input class="form-control form-control-lg" id="password" name="password" type="password" minlength="6" required>
                </div>
                <div class="col-12 d-grid">
                    <button class="btn btn-primary btn-lg" type="submit">Criar usuário</button>
                </div>
            </form>
        </div>
    </div>

    <div class="col-lg-8">
        <div class="panel-card p-4 h-100">
            <div class="d-flex flex-column flex-lg-row justify-content-between gap-3 align-items-lg-end mb-3">
                <div>
                    <h2 class="section-title h4 mb-1">Usuários cadastrados</h2>
                    <p class="text-secondary mb-0"><?= $isAdmin ? 'A listagem pode ser filtrada por empresa.' : 'A listagem mostra apenas usuários da sua empresa.' ?></p>
                </div>
                <form method="get" action="<?= $isAdmin ? '/admin/usuarios' : '/usuarios' ?>" class="row g-2 align-items-end">
                    <?php if ($isAdmin): ?>
                        <div class="col-auto">
                            <label class="form-label small mb-1" for="company_id_filter">Empresa</label>
                            <input class="form-control" id="company_id_filter" name="company_id" type="number" min="1" value="<?= e((string) ($companyIdFilter ?? '')) ?>">
                        </div>
                    <?php endif; ?>
                    <div class="col-auto">
                        <label class="form-label small mb-1" for="nome">Nome</label>
                        <input class="form-control" id="nome" name="nome" type="text" value="<?= e((string) ($searchName ?? '')) ?>">
                    </div>
                    <div class="col-auto">
                        <button class="btn btn-outline-dark" type="submit">Filtrar</button>
                    </div>
                </form>
            </div>

            <div class="table-responsive">
                <table class="table align-middle">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <?php if ($isAdmin): ?><th>Empresa</th><?php endif; ?>
                            <th>Nome</th>
                            <th>E-mail</th>
                            <th>Status</th>
                            <th class="text-end">Ações</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php if (empty($users)): ?>
                            <tr>
                                <td colspan="<?= $isAdmin ? '6' : '5' ?>" class="text-center text-secondary py-4">Nenhum usuário encontrado.</td>
                            </tr>
                        <?php else: ?>
                            <?php foreach ($users as $user): ?>
                                <tr>
                                    <td><?= e((string) ($user['id'] ?? '-')) ?></td>
                                    <?php if ($isAdmin): ?><td><?= e((string) ($user['companyId'] ?? '-')) ?></td><?php endif; ?>
                                    <td><?= e((string) ($user['name'] ?? '-')) ?></td>
                                    <td><?= e((string) ($user['email'] ?? '-')) ?></td>
                                    <td>
                                        <?php if (!empty($user['active'])): ?>
                                            <span class="badge text-bg-success">Ativo</span>
                                        <?php else: ?>
                                            <span class="badge text-bg-secondary">Inativo</span>
                                        <?php endif; ?>
                                    </td>
                                    <td class="text-end">
                                        <div class="d-flex justify-content-end gap-2 flex-wrap">
                                            <a class="btn btn-sm btn-outline-dark" href="<?= $isAdmin ? '/admin/usuarios/editar?id=' : '/usuarios/editar?id=' ?><?= e((string) ($user['id'] ?? '')) ?>">Editar</a>
                                            <form method="post" action="<?= $isAdmin ? '/admin/usuarios/acao' : '/usuarios/acao' ?>">
                                                <input type="hidden" name="id" value="<?= e((string) ($user['id'] ?? '')) ?>">
                                                <input type="hidden" name="action" value="<?= !empty($user['active']) ? 'desativar' : 'ativar' ?>">
                                                <button class="btn btn-sm <?= !empty($user['active']) ? 'btn-outline-danger' : 'btn-outline-success' ?>" type="submit">
                                                    <?= !empty($user['active']) ? 'Desativar' : 'Ativar' ?>
                                                </button>
                                            </form>
                                            <?php if ($isAdmin): ?>
                                                <form method="post" action="/admin/usuarios/acao" onsubmit="return confirm('Excluir este usuário?');">
                                                    <input type="hidden" name="id" value="<?= e((string) ($user['id'] ?? '')) ?>">
                                                    <input type="hidden" name="action" value="excluir">
                                                    <button class="btn btn-sm btn-outline-danger" type="submit">Excluir</button>
                                                </form>
                                            <?php endif; ?>
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
