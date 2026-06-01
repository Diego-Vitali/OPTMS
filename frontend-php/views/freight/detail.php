<?php
$detail = $detail ?? [];
$tabela = is_array($detail['tabela'] ?? null) ? $detail['tabela'] : [];
$objetos = is_array($detail['objetos'] ?? null) ? $detail['objetos'] : [];
$isAdmin = !empty($isAdmin);
$companyId = (string) (($form['company_id'] ?? ''));
$backUrl = $isAdmin ? '/admin/tabelas-frete' . ($companyId !== '' ? '?company_id=' . urlencode($companyId) : '') : '/tabelas-frete';

$formatValue = static function (mixed $value): string {
    if ($value === null || $value === '') {
        return '-';
    }
    if (is_float($value) || is_int($value)) {
        return number_format((float) $value, 2, ',', '.');
    }
    if (is_array($value)) {
        return implode('-', array_map(static fn (mixed $part): string => (string) $part, $value));
    }
    return (string) $value;
};
?>

<section class="panel-card p-4 p-lg-5 mb-4">
    <div class="d-flex justify-content-between align-items-start flex-wrap gap-3">
        <div>
            <span class="hero-badge mb-3">Tabela de frete</span>
            <h1 class="section-title h3 mb-2"><?= e((string) ($tabela['nome'] ?? 'Tabela')) ?></h1>
            <p class="text-secondary mb-0">
                ID <?= e((string) ($tabela['id'] ?? '-')) ?> |
                Tipo <?= e((string) ($tabela['tipo'] ?? '-')) ?> |
                Vigência <?= e($formatValue($tabela['vigenciaInicio'] ?? null)) ?> a <?= e($formatValue($tabela['vigenciaFim'] ?? null)) ?>
            </p>
        </div>
        <a class="btn btn-outline-dark" href="<?= e($backUrl) ?>">Voltar</a>
    </div>

    <?php if (!empty($error)): ?>
        <div class="alert alert-danger mt-4 mb-0"><?= e((string) $error) ?></div>
    <?php endif; ?>
</section>

<section class="panel-card p-4">
    <div class="d-flex justify-content-between align-items-center flex-wrap gap-3 mb-3">
        <div>
            <h2 class="section-title h4 mb-1">Regras cadastradas</h2>
            <p class="text-secondary mb-0">Faixas, constantes, unidade variante e método de cálculo usados nas cotações.</p>
        </div>
        <span class="stat-chip"><?= e((string) count($objetos)) ?> objetos</span>
    </div>

    <div class="table-responsive">
        <table class="table align-middle">
            <thead>
                <tr>
                    <th>Tipo</th>
                    <th>Componente</th>
                    <th>Origem</th>
                    <th>Destino</th>
                    <th>Unidade</th>
                    <th>Método</th>
                    <th>Faixa inicial</th>
                    <th>Faixa final</th>
                    <th>Valor</th>
                    <th>Excedente</th>
                </tr>
            </thead>
            <tbody>
                <?php if (empty($objetos)): ?>
                    <tr><td colspan="10" class="text-center text-secondary py-4">Nenhuma regra cadastrada.</td></tr>
                <?php else: ?>
                    <?php foreach ($objetos as $objeto): ?>
                        <?php
                        $faixas = is_array($objeto['faixas'] ?? null) ? $objeto['faixas'] : [];
                        $regras = is_array($faixas['regras'] ?? null) ? $faixas['regras'] : [];
                        if (empty($regras)) {
                            $regras = [[
                                'faixaInicial' => null,
                                'faixaFinal' => null,
                                'valor' => null,
                            ]];
                        }
                        ?>
                        <?php foreach ($regras as $regra): ?>
                            <tr>
                                <td><?= e((string) ($objeto['tipoObjeto'] ?? '-')) ?></td>
                                <td><?= e((string) ($objeto['nomeComponente'] ?? '-')) ?></td>
                                <td><?= e((string) ($objeto['ufOrigem'] ?? '*')) ?></td>
                                <td><?= e((string) ($objeto['ufDestino'] ?? $objeto['uf'] ?? '*')) ?></td>
                                <td><?= e((string) ($faixas['unidadeVariante'] ?? $objeto['baseCalculo'] ?? '-')) ?></td>
                                <td><?= e((string) ($faixas['metodoCalculo'] ?? $objeto['tipoCalculo'] ?? '-')) ?></td>
                                <td><?= e($formatValue($regra['faixaInicial'] ?? null)) ?></td>
                                <td><?= e($formatValue($regra['faixaFinal'] ?? null)) ?></td>
                                <td><?= e($formatValue($regra['valor'] ?? null)) ?></td>
                                <td><?= e((string) ($faixas['politicaExcedente'] ?? 'TOTAL')) ?></td>
                            </tr>
                        <?php endforeach; ?>
                    <?php endforeach; ?>
                <?php endif; ?>
            </tbody>
        </table>
    </div>
</section>
