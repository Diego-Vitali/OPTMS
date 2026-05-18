<?php $isAdmin = !empty($isAdmin); ?>

<section class="row justify-content-center py-4">
    <div class="col-xl-8">
        <div class="panel-card p-4 p-lg-5">
            <span class="hero-badge mb-3"><?= $isAdmin ? 'Admin' : 'Interno' ?></span>
            <h1 class="section-title h3 mb-3">Editar usuário</h1>
            <p class="text-secondary mb-4">Atualize os dados do usuário. A senha é opcional e só será alterada se você preencher o campo.</p>

            <?php if (!empty($error)): ?>
                <div class="alert alert-danger"><?= e((string) $error) ?></div>
            <?php endif; ?>

            <form method="post" action="<?= $isAdmin ? '/admin/usuarios/editar' : '/usuarios/editar' ?>" class="row g-3">
                <input type="hidden" name="id" value="<?= e((string) ($user['id'] ?? '')) ?>">
                <?php if ($isAdmin): ?>
                    <div class="col-12">
                        <label class="form-label" for="company_id">ID da empresa</label>
                        <input class="form-control form-control-lg" id="company_id" name="company_id" type="number" min="1" required value="<?= e((string) ($user['companyId'] ?? '')) ?>">
                    </div>
                <?php endif; ?>
                <div class="col-md-6">
                    <label class="form-label" for="name">Nome</label>
                    <input class="form-control form-control-lg" id="name" name="name" type="text" required value="<?= e((string) ($user['name'] ?? '')) ?>">
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="email">E-mail</label>
                    <input class="form-control form-control-lg" id="email" name="email" type="email" required value="<?= e((string) ($user['email'] ?? '')) ?>">
                </div>
                <div class="col-12">
                    <label class="form-label" for="password">Nova senha</label>
                    <input class="form-control form-control-lg" id="password" name="password" type="password" minlength="6" placeholder="Deixe em branco para manter a senha atual">
                </div>
                <div class="col-12 d-flex flex-column flex-md-row gap-3">
                    <button class="btn btn-primary btn-lg px-4" type="submit">Salvar alterações</button>
                    <a class="btn btn-outline-dark btn-lg px-4" href="<?= $isAdmin ? '/admin/usuarios' : '/usuarios' ?>">Voltar</a>
                </div>
            </form>
        </div>
    </div>
</section>
