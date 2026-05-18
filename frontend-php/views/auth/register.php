<section class="row g-4 align-items-stretch py-4">
    <div class="col-lg-5">
        <div class="hero-shell p-4 p-lg-5 h-100">
            <span class="hero-badge mb-4">Cadastro</span>
            <h1 class="display-6 fw-bold mb-3">Crie um usuário vinculado à company correta.</h1>
            <p class="lead text-white-50 mb-4">
                O cadastro usa a API key da empresa para associar o usuário no back-end do Spring sem acoplamento com o banco.
            </p>
            <ul class="list-unstyled mb-0 text-white-50">
                <li class="mb-2">Nome e e-mail do usuário</li>
                <li class="mb-2">Senha armazenada com hash BCrypt</li>
                <li>Validação feita pela própria API</li>
            </ul>
        </div>
    </div>
    <div class="col-lg-7">
        <div class="panel-card p-4 p-lg-5 h-100">
            <h2 class="section-title h3 mb-3">Cadastro de usuário</h2>
            <p class="text-secondary mb-4">Preencha os dados abaixo e use a API key da company que será dona desse usuário.</p>

            <?php if (!empty($error)): ?>
                <div class="alert alert-danger"><?= e((string) $error) ?></div>
            <?php endif; ?>

            <form method="post" action="/cadastro" class="row g-3">
                <div class="col-md-6">
                    <label class="form-label" for="name">Nome</label>
                    <input class="form-control form-control-lg" id="name" name="name" type="text" required value="<?= e($form['name'] ?? '') ?>">
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="email">E-mail</label>
                    <input class="form-control form-control-lg" id="email" name="email" type="email" required value="<?= e($form['email'] ?? '') ?>">
                </div>
                <div class="col-12">
                    <label class="form-label" for="password">Senha</label>
                    <input class="form-control form-control-lg" id="password" name="password" type="password" minlength="6" required>
                </div>
                <div class="col-12">
                    <label class="form-label" for="company_api_key">API key da company</label>
                    <textarea class="form-control" id="company_api_key" name="company_api_key" rows="3" required><?= e($form['company_api_key'] ?? '') ?></textarea>
                </div>
                <div class="col-12 d-flex flex-column flex-md-row gap-3">
                    <button class="btn btn-primary btn-lg px-4" type="submit">Criar usuário</button>
                    <a class="btn btn-outline-dark btn-lg px-4" href="/login">Voltar para o login</a>
                </div>
            </form>
        </div>
    </div>
</section>
