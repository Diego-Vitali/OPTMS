<?php $company = $company ?? []; ?>

<section class="row justify-content-center py-4">
    <div class="col-xl-8">
        <div class="panel-card p-4 p-lg-5">
            <span class="hero-badge mb-3">Admin</span>
            <h1 class="section-title h3 mb-3">Editar empresa</h1>
            <p class="text-secondary mb-4">Atualize os dados principais da empresa. A chave atual permanece a mesma.</p>

            <?php if (!empty($error)): ?>
                <div class="alert alert-danger"><?= e((string) $error) ?></div>
            <?php endif; ?>

            <form method="post" action="/admin/empresas/editar" class="row g-3">
                <input type="hidden" name="id" value="<?= e((string) ($company['id'] ?? '')) ?>">
                <div class="col-md-6">
                    <label class="form-label" for="name">Nome</label>
                    <input class="form-control form-control-lg" id="name" name="name" type="text" required value="<?= e((string) ($company['name'] ?? '')) ?>">
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="social_name">Razão social</label>
                    <input class="form-control form-control-lg" id="social_name" name="social_name" type="text" value="<?= e((string) ($company['socialName'] ?? '')) ?>">
                </div>
                <div class="col-12">
                    <label class="form-label" for="document">Documento</label>
                    <input class="form-control form-control-lg" id="document" name="document" type="text" value="<?= e((string) ($company['document'] ?? '')) ?>">
                </div>
                <div class="col-12 d-flex flex-column flex-md-row gap-3">
                    <button class="btn btn-primary btn-lg px-4" type="submit">Salvar alterações</button>
                    <a class="btn btn-outline-dark btn-lg px-4" href="/admin/empresas">Voltar</a>
                </div>
            </form>
        </div>
    </div>
</section>
