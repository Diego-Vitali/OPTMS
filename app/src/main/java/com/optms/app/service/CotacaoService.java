package com.optms.app.service;

import com.optms.app.dto.CotacaoRequest;
import com.optms.app.dto.CotacaoResponse;
import com.optms.app.dto.CotacaoResponse.ComponenteItem;
import com.optms.app.dto.CotacaoResponse.TabelaCotacaoItem;
import com.optms.app.model.ConfiguracaoCalculoFrete;
import com.optms.app.model.ObjetoFrete;
import com.optms.app.model.RegraCalculo;
import com.optms.app.model.TabelaFrete;
import com.optms.app.repository.ObjetoFreteRepository;
import com.optms.app.repository.TabelaFreteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

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
        String ufOrigem = normalizeUf(req.getUfOrigem());
        String ufDestino = normalizeUf(req.getUfDestino());
        List<TabelaFrete> tabelas = tabelaFreteRepository.findByCompanyIdAndAtivaTrue(companyId).stream()
                .filter(tabela -> containsUf(tabela.getUfsOrigem(), ufOrigem))
                .sorted((left, right) -> Long.compare(
                        right.getId() == null ? 0L : right.getId(),
                        left.getId() == null ? 0L : left.getId()
                ))
                .toList();

        if (tabelas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Nenhuma tabela de frete ativa para UF origem: " + ufOrigem);
        }

        List<TabelaCotacaoItem> cotacoes = tabelas.stream()
                .map(tabela -> calcularPorTabela(req, tabela, ufOrigem, ufDestino))
                .toList();

        return CotacaoResponse.builder()
                .ufOrigem(ufOrigem)
                .ufDestino(ufDestino)
                .peso(req.getPeso())
                .valorNF(req.getValorNF())
                .cotacoes(cotacoes)
                .build();
    }

    private TabelaCotacaoItem calcularPorTabela(CotacaoRequest req, TabelaFrete tabela, String ufOrigem, String ufDestino) {
        List<ObjetoFrete> objetos = objetoFreteRepository.findByTabelaId(tabela.getId());

        ObjetoFrete partida = objetos.stream()
                .filter(o -> "PARTIDA".equals(o.getTipoObjeto()))
                .filter(o -> appliesToRoute(o, ufOrigem, ufDestino))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(422),
                        "Tabela sem objeto PARTIDA: " + tabela.getNome()));

        double freteBase = resolverValor(partida, req.getPeso(), req.getValorNF(), 0.0);

        List<ComponenteItem> componentes = objetos.stream()
                .filter(o -> "COMPONENTE".equals(o.getTipoObjeto()))
                .filter(o -> appliesToRoute(o, ufOrigem, ufDestino))
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
     * Determina o valor do objeto a partir de uma regra constante ou da faixa aplicável.
     */
    private double resolverValor(ObjetoFrete obj, Double peso, Double valorNF, double freteBase) {
        ConfiguracaoCalculoFrete config = obj.getConfigCalculo();
        if (config == null) return 0.0;

        Double entrada = resolverBase(config.getUnidadeFaixa(), peso, valorNF, freteBase);
        RegraCalculo regra = config.buscarRegra(entrada);
        if (regra == null || regra.getValorCalculo() == null) return 0.0;

        double base = resolverBase(regra.getUnidadeVariante(), peso, valorNF, freteBase);
        return switch (String.valueOf(regra.getTipoCalculo()).toUpperCase(Locale.ROOT)) {
            case "PERCENTUAL" -> base * (regra.getValorCalculo() / 100.0);
            case "MULTIPLICADOR" -> base * regra.getValorCalculo();
            default -> regra.getValorCalculo();
        };
    }

    private double resolverBase(String unidade, Double peso, Double valorNF, double freteBase) {
        return switch (String.valueOf(unidade).toUpperCase(Locale.ROOT)) {
            case "PESO_BRUTO" -> peso != null ? peso : 0.0;
            case "VALOR_FRETE_PARTIDA" -> freteBase;
            default -> valorNF != null ? valorNF : 0.0;
        };
    }

    private boolean containsUf(List<String> ufs, String uf) {
        return ufs != null && ufs.stream().anyMatch(value -> uf.equalsIgnoreCase(value));
    }

    private boolean appliesToRoute(ObjetoFrete objeto, String ufOrigem, String ufDestino) {
        return ufOrigem.equalsIgnoreCase(String.valueOf(objeto.getUfOrigem()))
                && ufDestino.equalsIgnoreCase(String.valueOf(objeto.getUfDestino()));
    }

    private String normalizeUf(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
