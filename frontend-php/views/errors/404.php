<section class="row justify-content-center py-5">
    <div class="col-lg-7">
        <div class="panel-card p-5 text-center">
            <span class="hero-badge mb-3">404</span>
            <h1 class="section-title h2 mb-3">Página não encontrada</h1>
            <p class="text-secondary mb-4">A rota solicitada não existe neste front-end em PHP.</p>
            <a class="btn btn-primary btn-lg px-4" href="<?= is_authenticated() ? '/dashboard' : '/login' ?>">Ir para a tela principal</a>
        </div>
    </div>
</section>
