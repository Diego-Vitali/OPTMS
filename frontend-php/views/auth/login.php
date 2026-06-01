<section class="row g-4 align-items-stretch py-4">
    <div class="col-lg-6">
        <div class="hero-shell p-4 p-lg-5 h-100">
            <span class="hero-badge mb-4">Portal</span>
            <h1 class="display-6 fw-bold mb-3">Acesse o <?= e(app_name()) ?> com o seu usuário.</h1>
            <p class="lead text-white-50 mb-4">
                <?= e(app_slogan()) ?>
            </p>
            <div class="d-flex flex-wrap gap-2">
                <span class="stat-chip">Login por usuário</span>
                <span class="stat-chip">Sessão protegida</span>
                <span class="stat-chip">Ambiente seguro</span>
            </div>
        </div>
    </div>
    <div class="col-lg-6">
        <div class="panel-card p-4 p-lg-5 h-100">
            <h2 class="section-title h3 mb-3">Entrar</h2>
            <p class="text-secondary mb-4">Use o e-mail do usuário ou o login fixo <strong>admin</strong> para a área administrativa.</p>

            <?php if (!empty($error)): ?>
                <div class="alert alert-danger"><?= e((string) $error) ?></div>
            <?php endif; ?>

            <form method="post" action="/login" class="row g-3">
                <div class="col-12">
                    <label class="form-label" for="login">Login ou e-mail</label>
                    <input class="form-control form-control-lg" id="login" name="login" type="text" required value="<?= e($form['login'] ?? '') ?>">
                </div>
                <div class="col-12">
                    <label class="form-label" for="password">Senha</label>
                    <input class="form-control form-control-lg" id="password" name="password" type="password" required>
                </div>
                <div class="col-12 d-grid">
                    <button class="btn btn-primary btn-lg cta-button" type="submit">Entrar no painel</button>
                </div>
            </form>
        </div>
    </div>
</section>
