<section class="row g-4">
    <div class="col-lg-7">
        <div class="panel-card p-4 p-lg-5">
            <span class="hero-badge mb-3">Upload XLSX</span>
            <h1 class="section-title h2 mb-3">Tabela de frete por arquivo</h1>
            <p class="text-secondary mb-4">
                Envie uma planilha `.xlsx` com as abas <strong>Config</strong> e <strong>Tabela</strong>. O Spring converte o conteúdo para `TabelaFreteRequest` e persiste os objetos calculáveis.
            </p>

            <?php if (!empty($error)): ?>
                <div class="alert alert-danger"><?= e((string) $error) ?></div>
            <?php endif; ?>

            <?php if (!empty($result)): ?>
                <div class="alert alert-success">
                    <strong><?= e((string) ($result['message'] ?? 'Upload concluído.')) ?></strong><br>
                    Tabela ID: <?= e((string) ($result['tabelaId'] ?? '-')) ?> |
                    UFs origem: <?= e(implode(', ', $result['ufsOrigem'] ?? [])) ?> |
                    Objetos criados: <?= e((string) ($result['objetosCriados'] ?? '0')) ?>
                </div>
            <?php endif; ?>

            <form method="post" action="<?= !empty($isAdmin) ? '/admin/tabelas-frete/upload' : '/tabelas-frete/upload' ?>" enctype="multipart/form-data" class="row g-3">
                <?php if (!empty($isAdmin)): ?>
                    <div class="col-12">
                        <label class="form-label" for="company_id">ID da empresa</label>
                        <input class="form-control form-control-lg" id="company_id" name="company_id" type="number" min="1" required value="<?= e($form['company_id'] ?? '') ?>">
                    </div>
                <?php endif; ?>
                <div class="col-12">
                    <label class="form-label" for="file">Arquivo da tabela de frete</label>
                    <input class="form-control form-control-lg" id="file" name="file" type="file" accept=".xlsx" required>
                </div>
                <div class="col-12 d-flex gap-3">
                    <button class="btn btn-primary btn-lg px-4" type="submit">Enviar planilha</button>
                    <a class="btn btn-outline-dark btn-lg px-4" href="<?= !empty($isAdmin) ? '/admin' : '/dashboard' ?>">Voltar</a>
                </div>
            </form>
        </div>
    </div>
    <div class="col-lg-5">
        <div class="info-card p-4 h-100">
            <h2 class="section-title h5 mb-3">Formato esperado</h2>
            <div class="list-spec">
                <p><strong>Aba Config</strong>: `Nome Referência`, `Vigência Início`, `Vigência Fim`.</p>
                <p><strong>Aba Frete_Partida</strong>: `UF Origem`, `UF Destino`, `Forma Calculo`, `Unidade Faixa`, `Limite Inicial`, `Limite Final`, `Unidade variante`, `Tipo Calculo`, `Valor do cálculo`.</p>
                <p><strong>Aba Componentes</strong>: mesmas colunas de rota e cálculo, adicionando `Nome Componente`.</p>
                <?php if (!empty($isAdmin)): ?>
                    <p><strong>Modo admin</strong>: informe o ID da empresa que deve receber a tabela.</p>
                <?php endif; ?>
                <p class="mb-0"><strong>Rotas</strong>: cada linha representa uma origem e um destino específicos.</p>
            </div>
        </div>
    </div>
</section>
