<?php
$form = $form ?? [];
$result = $result ?? [];
$isAdmin = !empty($isAdmin);
?>

<section class="row g-4">
    <div class="col-lg-7">
        <div class="panel-card p-4 p-lg-5">
            <span class="hero-badge mb-3">Cotação</span>
            <h1 class="section-title h2 mb-3">Simular frete por tabela</h1>
            <p class="text-secondary mb-4">
                Informe origem, destino, peso e valor da nota para calcular o frete com base nas tabelas cadastradas.
            </p>

            <?php if (!empty($error)): ?>
                <div class="alert alert-danger"><?= e((string) $error) ?></div>
            <?php endif; ?>

            <form method="post" action="<?= $isAdmin ? '/admin/cotacoes' : '/cotacoes' ?>" class="row g-3">
                <?php if ($isAdmin): ?>
                    <div class="col-12">
                        <label class="form-label" for="company_id">ID da empresa</label>
                        <input class="form-control form-control-lg" id="company_id" name="company_id" type="number" min="1" required value="<?= e($form['company_id'] ?? '') ?>">
                    </div>
                <?php endif; ?>
                <div class="col-md-6">
                    <label class="form-label" for="uf_origem">UF de origem</label>
                    <input class="form-control form-control-lg" id="uf_origem" name="uf_origem" type="text" maxlength="2" required value="<?= e($form['uf_origem'] ?? '') ?>">
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="uf_destino">UF de destino</label>
                    <input class="form-control form-control-lg" id="uf_destino" name="uf_destino" type="text" maxlength="2" required value="<?= e($form['uf_destino'] ?? '') ?>">
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="peso">Peso total (kg)</label>
                    <input class="form-control form-control-lg" id="peso" name="peso" type="number" step="0.01" min="0.01" required value="<?= e($form['peso'] ?? '') ?>">
                </div>
                <div class="col-md-6">
                    <label class="form-label" for="valor_nf">Valor da NF</label>
                    <input class="form-control form-control-lg" id="valor_nf" name="valor_nf" type="number" step="0.01" min="0" required value="<?= e($form['valor_nf'] ?? '') ?>">
                </div>
                <div class="col-12 d-flex gap-3">
                    <button class="btn btn-primary btn-lg px-4" type="submit">Calcular cotação</button>
                    <a class="btn btn-outline-dark btn-lg px-4" href="<?= $isAdmin ? '/admin' : '/dashboard' ?>">Voltar</a>
                </div>
            </form>
        </div>
    </div>
    <div class="col-lg-5">
        <div class="info-card p-4 h-100">
            <h2 class="section-title h5 mb-3">Resultado</h2>
            <?php if (empty($result['cotacoes'])): ?>
                <p class="list-spec mb-0">A API retornará uma cotação por tabela ativa encontrada para a UF de origem informada.</p>
            <?php else: ?>
                <div class="list-spec mb-3">
                    <p class="mb-1"><strong>Origem:</strong> <?= e((string) ($result['ufOrigem'] ?? '-')) ?></p>
                    <p class="mb-1"><strong>Destino:</strong> <?= e((string) ($result['ufDestino'] ?? '-')) ?></p>
                    <p class="mb-1"><strong>Peso:</strong> <?= e((string) ($result['peso'] ?? '-')) ?></p>
                    <p class="mb-0"><strong>Valor NF:</strong> <?= e((string) ($result['valorNF'] ?? '-')) ?></p>
                </div>
                <?php foreach (($result['cotacoes'] ?? []) as $cotacao): ?>
                    <div class="quote-card mb-3">
                        <div class="d-flex justify-content-between align-items-start gap-3">
                            <div>
                                <h3 class="h6 mb-1"><?= e((string) ($cotacao['tabelaNome'] ?? 'Tabela')) ?></h3>
                                <div class="text-secondary small">Tabela ID <?= e((string) ($cotacao['tabelaId'] ?? '-')) ?></div>
                            </div>
                            <div class="text-end">
                                <div class="small text-secondary">Total</div>
                                <div class="fw-bold fs-5">R$ <?= e(number_format((float) ($cotacao['total'] ?? 0), 2, ',', '.')) ?></div>
                            </div>
                        </div>
                        <hr>
                        <p class="mb-2"><strong>Frete base:</strong> R$ <?= e(number_format((float) ($cotacao['freteBase'] ?? 0), 2, ',', '.')) ?></p>
                        <?php if (!empty($cotacao['componentes']) && is_array($cotacao['componentes'])): ?>
                            <table class="table table-sm align-middle mb-0">
                                <thead>
                                    <tr>
                                        <th>Componente</th>
                                        <th class="text-end">Valor</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <?php foreach ($cotacao['componentes'] as $componente): ?>
                                        <tr>
                                            <td><?= e((string) ($componente['nome'] ?? '-')) ?></td>
                                            <td class="text-end">R$ <?= e(number_format((float) ($componente['valor'] ?? 0), 2, ',', '.')) ?></td>
                                        </tr>
                                    <?php endforeach; ?>
                                </tbody>
                            </table>
                        <?php endif; ?>
                    </div>
                <?php endforeach; ?>
            <?php endif; ?>
        </div>
    </div>
</section>
