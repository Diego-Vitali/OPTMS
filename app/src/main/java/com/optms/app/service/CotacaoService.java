package com.optms.app.service;

import com.optms.app.dto.CotacaoRequest;
import com.optms.app.dto.CotacaoResponse;
import com.optms.app.dto.CotacaoResponse.ComponenteItem;
import com.optms.app.dto.CotacaoResponse.TabelaCotacaoItem;
import com.optms.app.model.ObjetoFrete;
import com.optms.app.model.TabelaFrete;
import com.optms.app.repository.ObjetoFreteRepository;
import com.optms.app.repository.TabelaFreteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Motor de cálculo de cotação de frete.
 *
 * Fluxo:
 *   1. Localiza a tabela ativa pela UF de origem.
 *   2. Calcula o frete base com o objeto PARTIDA.
 *   3. Soma os COMPONENTEs aplicáveis ao destino.
 */
@Service
@RequiredArgsConstructor
public class CotacaoService {

    private final TabelaFreteRepository tabelaFreteRepository;
    private final ObjetoFreteRepository objetoFreteRepository;

    public CotacaoResponse calcular(CotacaoRequest req, Long companyId) {
        List<TabelaFrete> tabelas = tabelaFreteRepository
                .findByCompanyIdAndUfOrigemAndAtivaTrueOrderByIdDesc(companyId, req.getUfOrigem());

        if (tabelas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Nenhuma tabela de frete ativa para UF origem: " + req.getUfOrigem());
        }

        List<TabelaCotacaoItem> cotacoes = tabelas.stream()
                .map(tabela -> calcularPorTabela(req, tabela))
                .toList();

        return CotacaoResponse.builder()
                .ufOrigem(req.getUfOrigem())
                .ufDestino(req.getUfDestino())
                .peso(req.getPeso())
                .valorNF(req.getValorNF())
                .cotacoes(cotacoes)
                .build();
    }

    private TabelaCotacaoItem calcularPorTabela(CotacaoRequest req, TabelaFrete tabela) {
        List<ObjetoFrete> objetos = objetoFreteRepository.findByTabelaId(tabela.getId());

        ObjetoFrete partida = objetos.stream()
                .filter(o -> "PARTIDA".equals(o.getTipoObjeto()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(422),
                        "Tabela sem objeto PARTIDA: " + tabela.getNome()));

        double freteBase = resolverValor(partida, req.getPeso(), req.getValorNF(), 0.0);

        List<ComponenteItem> componentes = objetos.stream()
                .filter(o -> "COMPONENTE".equals(o.getTipoObjeto()))
                .filter(o -> o.getUf() == null || o.getUf().equalsIgnoreCase(req.getUfDestino()))
                .map(comp -> {
                    double valor = resolverValor(comp, req.getPeso(), req.getValorNF(), freteBase);
                    return new ComponenteItem(comp.getNomeComponente(), valor);
                })
                .toList();

        double total = freteBase + componentes.stream().mapToDouble(ComponenteItem::valor).sum();

        return new TabelaCotacaoItem(
                tabela.getId(),
                tabela.getNome(),
                freteBase,
                componentes,
                total
        );
    }

    /**
     * Determina o valor do objeto com base na sua configuração:
     * - sobreFretePartida=true → usa freteBase como entrada nas faixas
     * - tipoFaixa PESO         → usa o peso como entrada
     * - tipoFaixa VLR_NF       → usa o valor da NF como entrada
     */
    private double resolverValor(ObjetoFrete obj, double peso, double valorNF, double freteBase) {
        if (obj.getFaixas() == null) return 0.0;

        double entrada;
        if (obj.isSobreFretePartida()) {
            entrada = freteBase;
        } else {
            entrada = "PESO".equalsIgnoreCase(obj.getFaixas().getTipoFaixa()) ? peso : valorNF;
        }

        Double valor = obj.getFaixas().buscarValor(entrada);
        return valor != null ? valor : 0.0;
    }
}
