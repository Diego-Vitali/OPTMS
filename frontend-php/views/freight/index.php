<?php
$tables = $tables ?? [];
$form = $form ?? ['company_id' => ''];
$isAdmin = !empty($isAdmin);

$formatList = static function (array|null $values, string $empty = 'Todas'): string {
    if (empty($values)) {
        return $empty;
    }
    return implode(', ', array_map(static fn ($value) => (string) $value, $values));
};

$label = static function (string|null $value): string {
    return match ($value) {
        'PESO_BRUTO' => 'Peso bruto',
        'VALOR_NOTA' => 'Valor da nota',
        'VALOR_FRETE_PARTIDA' => 'Frete partida',
        'VALOR_FIXO' => 'Valor fixo',
        'PERCENTUAL' => 'Percentual',
        'MULTIPLICADOR' => 'Multiplicador',
        'FAIXA' => 'Por faixa',
        'CONSTANTE' => 'Constante',
        default => (string) ($value ?? '-'),
    };
};

$formatLimit = static function (mixed $value): string {
    if ($value === null || $value === '') {
        return 'sem limite';
    }
    return number_format((float) $value, 2, ',', '.');
};
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
                            <th>UFs origem</th>
                            <th>UFs destino</th>
                            <th>Vigência</th>
                            <th>Status</th>
                            <th class="text-end">Ações</th>
                        </tr>
                    </thead>
                    <tbody>
                        <?php if (empty($tables)): ?>
                            <tr>
                                <td colspan="<?= $isAdmin ? '8' : '7' ?>" class="text-center text-secondary py-4">Nenhuma tabela encontrada.</td>
                            </tr>
                        <?php else: ?>
                            <?php foreach ($tables as $table): ?>
                                <tr>
                                    <td><?= e((string) ($table['id'] ?? '-')) ?></td>
                                    <?php if ($isAdmin): ?><td><?= e((string) ($table['companyId'] ?? '-')) ?></td><?php endif; ?>
                                    <td><?= e((string) ($table['nome'] ?? '-')) ?></td>
                                    <td><?= e($formatList($table['ufsOrigem'] ?? [], '-')) ?></td>
                                    <td><?= e($formatList($table['ufsDestino'] ?? [])) ?></td>
                                    <td><?= e((string) ($table['vigenciaInicio'] ?? '-')) ?> até <?= e((string) ($table['vigenciaFim'] ?? '-')) ?></td>
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
                                <tr>
                                    <td colspan="<?= $isAdmin ? '8' : '7' ?>" class="bg-light">
                                        <?php $objects = $table['objetos'] ?? []; ?>
                                        <?php if (empty($objects)): ?>
                                            <span class="text-secondary">Nenhum objeto de frete cadastrado.</span>
                                        <?php else: ?>
                                            <div class="table-responsive">
                                                <table class="table table-sm mb-0 align-middle">
                                                    <thead>
                                                        <tr>
                                                            <th>Objeto</th>
                                                            <th>Nome</th>
                                                            <th>Origem</th>
                                                            <th>Destino</th>
                                                            <th>Forma</th>
                                                            <th>Faixa por</th>
                                                            <th>Regras</th>
                                                        </tr>
                                                    </thead>
                                                    <tbody>
                                                        <?php foreach ($objects as $object): ?>
                                                            <?php $config = $object['configCalculo'] ?? []; ?>
                                                            <tr>
                                                                <td><span class="badge text-bg-dark"><?= e((string) ($object['tipoObjeto'] ?? '-')) ?></span></td>
                                                                <td><?= e((string) ($object['nomeComponente'] ?? '-')) ?></td>
                                                                <td><?= e((string) ($object['ufOrigem'] ?? '-')) ?></td>
                                                                <td><?= e((string) ($object['ufDestino'] ?? '-')) ?></td>
                                                                <td><?= e($label($config['formaCalculo'] ?? null)) ?></td>
                                                                <td><?= e($label($config['unidadeFaixa'] ?? null)) ?></td>
                                                                <td>
                                                                    <?php foreach (($config['regras'] ?? []) as $rule): ?>
                                                                        <div class="small">
                                                                            <?= e($formatLimit($rule['limiteInicial'] ?? null)) ?> até <?= e($formatLimit($rule['limiteFinal'] ?? null)) ?>:
                                                                            <?= e($label($rule['tipoCalculo'] ?? null)) ?>
                                                                            <?= e(number_format((float) ($rule['valorCalculo'] ?? 0), 2, ',', '.')) ?>
                                                                            sobre <?= e($label($rule['unidadeVariante'] ?? null)) ?>
                                                                        </div>
                                                                    <?php endforeach; ?>
                                                                </td>
                                                            </tr>
                                                        <?php endforeach; ?>
                                                    </tbody>
                                                </table>
                                            </div>
                                        <?php endif; ?>
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
