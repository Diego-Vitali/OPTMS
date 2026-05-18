<section class="row g-4">
    <div class="col-lg-7">
        <div class="panel-card p-4 p-lg-5">
            <span class="hero-badge mb-3">Machine Learning</span>
            <h1 class="section-title h2 mb-3">Retreino do modelo</h1>
            <p class="text-secondary mb-4">
                Envie a base histórica `.xlsx` para o Spring montar o payload de retreino e acionar a FastAPI de ML.
            </p>

            <?php if (!empty($error)): ?>
                <div class="alert alert-danger"><?= e((string) $error) ?></div>
            <?php endif; ?>

            <?php if (!empty($result['error'])): ?>
                <div class="alert alert-warning">
                    <strong>Retreino recusado:</strong> <?= e((string) $result['error']) ?>
                </div>
            <?php elseif (!empty($result)): ?>
                <div class="alert alert-success">
                    <strong>Modelo atualizado com sucesso.</strong><br>
                    Registros válidos: <?= e((string) ($result['nRegistrosTreino'] ?? '-')) ?> |
                    Linhas descartadas: <?= e((string) ($result['linhasDescartadas'] ?? '0')) ?>
                </div>
            <?php endif; ?>

            <form method="post" action="/ml/retrain" enctype="multipart/form-data" class="row g-3">
                <div class="col-12">
                    <label class="form-label" for="file">Arquivo histórico para treinamento</label>
                    <input class="form-control form-control-lg" id="file" name="file" type="file" accept=".xlsx" required>
                </div>
                <div class="col-12 d-flex gap-3">
                    <button class="btn btn-primary btn-lg px-4" type="submit">Treinar modelo</button>
                    <a class="btn btn-outline-dark btn-lg px-4" href="<?= !empty($isAdmin) ? '/admin' : '/dashboard' ?>">Voltar</a>
                </div>
            </form>
        </div>
    </div>
    <div class="col-lg-5">
        <div class="info-card p-4 h-100">
            <h2 class="section-title h5 mb-3">Colunas obrigatórias</h2>
            <div class="list-spec mb-4">
                <p>`UF_ORIGEM`, `UF_DESTINO`, `PESO_BRUTO`, `METRO_CUBICO`, `VALOR_NF`, `QTD_VOLUMES`, `VIA_TRANSPORTE`, `TIPO_FRETE`, `TRANSIT_TIME_REAL`.</p>
            </div>

            <?php if (!empty($result) && empty($result['error'])): ?>
                <h3 class="h6 text-uppercase text-secondary fw-semibold">Métricas retornadas</h3>
                <table class="table align-middle">
                    <tbody>
                        <tr>
                            <th scope="row">MAE K-Fold</th>
                            <td><?= e((string) ($result['maeKfold'] ?? '-')) ?></td>
                        </tr>
                        <tr>
                            <th scope="row">RMSE K-Fold</th>
                            <td><?= e((string) ($result['rmseKfold'] ?? '-')) ?></td>
                        </tr>
                        <tr>
                            <th scope="row">R² K-Fold</th>
                            <td><?= e((string) ($result['r2Kfold'] ?? '-')) ?></td>
                        </tr>
                    </tbody>
                </table>
            <?php else: ?>
                <p class="list-spec mb-0">A resposta da API exibirá as métricas de treino e a quantidade de linhas descartadas após a limpeza dos dados.</p>
            <?php endif; ?>
        </div>
    </div>
</section>
