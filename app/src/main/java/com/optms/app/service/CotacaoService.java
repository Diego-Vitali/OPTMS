package com.optms.app.service;

import com.optms.app.dto.CotacaoRequest;
import com.optms.app.dto.CotacaoResponse;
import com.optms.app.dto.CotacaoResponse.ComponenteItem;
import com.optms.app.dto.CotacaoResponse.TabelaCotacaoItem;
import com.optms.app.model.FaixaCalculo;
import com.optms.app.model.FaixaCalculo.RegraAplicavel;
import com.optms.app.model.ObjetoFrete;
import com.optms.app.model.TabelaFrete;
import com.optms.app.repository.ObjetoFreteRepository;
import com.optms.app.repository.TabelaFreteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                .findByCompanyIdAndAtivaTrueOrderByIdDesc(companyId);

        if (tabelas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Nenhuma tabela de frete ativa encontrada");
        }

        List<TabelaCotacaoItem> cotacoes = tabelas.stream()
                .map(tabela -> calcularPorTabela(req, tabela))
                .flatMap(Optional::stream)
                .toList();

        if (cotacoes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Nenhuma tabela de frete ativa atende a rota " + req.getUfOrigem() + " > " + req.getUfDestino());
        }

        return CotacaoResponse.builder()
                .ufOrigem(req.getUfOrigem())
                .ufDestino(req.getUfDestino())
                .peso(req.getPeso())
                .valorNF(req.getValorNF())
                .cotacoes(cotacoes)
                .build();
    }

    private Optional<TabelaCotacaoItem> calcularPorTabela(CotacaoRequest req, TabelaFrete tabela) {
        List<ObjetoFrete> objetos = normalizarObjetos(objetoFreteRepository.findByTabelaId(tabela.getId()));

        ObjetoFrete partida = objetos.stream()
                .filter(o -> "PARTIDA".equals(o.getTipoObjeto()))
                .filter(o -> aplicaRota(o, req))
                .filter(o -> possuiRegraAplicavel(o, req, 0.0))
                .findFirst()
                .orElse(null);

        if (partida == null) {
            return Optional.empty();
        }

        double freteBase = resolverValor(partida, req.getPeso(), req.getValorNF(), 0.0);

        List<ComponenteItem> componentes = objetos.stream()
                .filter(o -> "COMPONENTE".equals(o.getTipoObjeto()))
                .filter(o -> aplicaRota(o, req))
                .filter(o -> possuiRegraAplicavel(o, req, freteBase))
                .map(comp -> {
                    double valor = resolverValor(comp, req.getPeso(), req.getValorNF(), freteBase);
                    return new ComponenteItem(comp.getNomeComponente(), valor);
                })
                .toList();

        double total = freteBase + componentes.stream().mapToDouble(ComponenteItem::valor).sum();

        return Optional.of(new TabelaCotacaoItem(
                tabela.getId(),
                tabela.getNome(),
                freteBase,
                componentes,
                total
        ));
    }

    /**
     * Determina o valor do objeto com base na sua configuração:
     * - sobreFretePartida=true → usa freteBase como entrada nas faixas
     * - tipoFaixa PESO         → usa o peso como entrada
     * - tipoFaixa VLR_NF       → usa o valor da NF como entrada
     */
    private double resolverValor(ObjetoFrete obj, double peso, double valorNF, double freteBase) {
        if (obj.getFaixas() == null) return 0.0;

        if (obj.getFaixas().usaNovaEstrutura()) {
            return resolverNovaEstrutura(obj.getFaixas(), peso, valorNF, freteBase)
                    .orElse(0.0);
        }

        double entrada;
        if (obj.isSobreFretePartida()) {
            entrada = freteBase;
        } else {
            entrada = "PESO".equalsIgnoreCase(obj.getFaixas().getTipoFaixa()) ? peso : valorNF;
        }

        Double valor = obj.getFaixas().buscarValor(entrada);
        if (valor == null) {
            return 0.0;
        }

        if ("PERCENTUAL".equalsIgnoreCase(obj.getTipoCalculo())) {
            double base = switch (String.valueOf(obj.getBaseCalculo()).toUpperCase()) {
                case "VLR_NF", "VALOR_NF" -> valorNF;
                case "PESO" -> peso;
                default -> obj.isSobreFretePartida() ? freteBase : valorNF;
            };
            double calculado = base * (valor / 100.0);
            Double minimo = obj.getFaixas().getMinimo();
            return minimo != null ? Math.max(calculado, minimo) : calculado;
        }

        return valor;
    }

    private Optional<Double> resolverNovaEstrutura(FaixaCalculo faixas, double peso, double valorNF, double freteBase) {
        double entrada = entradaPorUnidade(faixas.getUnidadeVariante(), peso, valorNF, freteBase);
        Optional<RegraAplicavel> regraAplicavel = faixas.buscarRegra(entrada);
        if (regraAplicavel.isEmpty() || regraAplicavel.get().regra().getValor() == null) {
            return Optional.empty();
        }

        double valor = regraAplicavel.get().regra().getValor();
        String metodo = String.valueOf(faixas.getMetodoCalculo()).toUpperCase();
        return Optional.of(switch (metodo) {
            case FaixaCalculo.METODO_MULTIPLICADOR -> regraAplicavel.get().baseCalculo() * valor;
            case FaixaCalculo.METODO_PERCENTUAL -> regraAplicavel.get().baseCalculo() * (valor / 100.0);
            case FaixaCalculo.METODO_VALOR_FIXO -> valor;
            default -> throw new ResponseStatusException(HttpStatusCode.valueOf(422),
                    "Método de cálculo inválido: " + faixas.getMetodoCalculo());
        });
    }

    private boolean possuiRegraAplicavel(ObjetoFrete obj, CotacaoRequest req, double freteBase) {
        if (obj.getFaixas() == null || !obj.getFaixas().usaNovaEstrutura()) {
            return true;
        }
        double entrada = entradaPorUnidade(obj.getFaixas().getUnidadeVariante(), req.getPeso(), req.getValorNF(), freteBase);
        return obj.getFaixas().buscarRegra(entrada).isPresent();
    }

    private double entradaPorUnidade(String unidade, double peso, double valorNF, double freteBase) {
        return switch (String.valueOf(unidade).toUpperCase()) {
            case FaixaCalculo.UNIDADE_PESO -> peso;
            case FaixaCalculo.UNIDADE_VALOR, "VALOR_NF" -> valorNF;
            case FaixaCalculo.UNIDADE_FRETE_PARTIDA -> freteBase;
            default -> valorNF;
        };
    }

    private boolean aplicaRota(ObjetoFrete obj, CotacaoRequest req) {
        String origem = obj.getUfOrigem();
        String destino = obj.getUfDestino() != null ? obj.getUfDestino() : obj.getUf();
        boolean origemOk = matchesUf(origem, req.getUfOrigem());
        boolean destinoOk = matchesUf(destino, req.getUfDestino());
        return origemOk && destinoOk;
    }

    private boolean matchesUf(String configured, String requested) {
        return configured == null
                || configured.isBlank()
                || "*".equals(configured)
                || "TODOS".equalsIgnoreCase(configured)
                || configured.equalsIgnoreCase(requested);
    }

    private List<ObjetoFrete> normalizarObjetos(List<ObjetoFrete> objetos) {
        Map<String, ObjetoFrete> agrupados = new LinkedHashMap<>();
        for (ObjetoFrete objeto : objetos) {
            if (objeto.getFaixas() == null || !objeto.getFaixas().usaNovaEstrutura()) {
                agrupados.put("LEGACY|" + objeto.getId(), objeto);
                continue;
            }

            String key = String.join("|",
                    String.valueOf(objeto.getTipoObjeto()),
                    String.valueOf(objeto.getNomeComponente()),
                    String.valueOf(objeto.getUfOrigem()),
                    String.valueOf(objeto.getUfDestino() != null ? objeto.getUfDestino() : objeto.getUf()),
                    String.valueOf(objeto.getFaixas().getUnidadeVariante()),
                    String.valueOf(objeto.getFaixas().getMetodoCalculo())
            );

            ObjetoFrete acumulado = agrupados.get(key);
            if (acumulado == null) {
                agrupados.put(key, copiarObjeto(objeto));
                continue;
            }

            if (maxFaixaFinal(objeto) >= maxFaixaFinal(acumulado)) {
                acumulado.getFaixas().setPoliticaExcedente(objeto.getFaixas().getPoliticaExcedente());
            }
            acumulado.getFaixas().getRegras().addAll(objeto.getFaixas().getRegras());
        }
        return new ArrayList<>(agrupados.values());
    }

    private double maxFaixaFinal(ObjetoFrete objeto) {
        if (objeto.getFaixas() == null || objeto.getFaixas().getRegras() == null) {
            return Double.NEGATIVE_INFINITY;
        }
        return objeto.getFaixas().getRegras().stream()
                .filter(regra -> regra.getFaixaFinal() != null)
                .mapToDouble(FaixaCalculo.Regra::getFaixaFinal)
                .max()
                .orElse(Double.POSITIVE_INFINITY);
    }

    private ObjetoFrete copiarObjeto(ObjetoFrete origem) {
        ObjetoFrete copia = new ObjetoFrete();
        copia.setId(origem.getId());
        copia.setTabelaId(origem.getTabelaId());
        copia.setUfOrigem(origem.getUfOrigem());
        copia.setUfDestino(origem.getUfDestino());
        copia.setUf(origem.getUf());
        copia.setTipoObjeto(origem.getTipoObjeto());
        copia.setBaseCalculo(origem.getBaseCalculo());
        copia.setTipoCalculo(origem.getTipoCalculo());
        copia.setNomeComponente(origem.getNomeComponente());
        copia.setSobreFretePartida(origem.isSobreFretePartida());

        FaixaCalculo faixasOrigem = origem.getFaixas();
        if (faixasOrigem != null) {
            FaixaCalculo faixas = new FaixaCalculo();
            faixas.setUnidadeVariante(faixasOrigem.getUnidadeVariante());
            faixas.setMetodoCalculo(faixasOrigem.getMetodoCalculo());
            faixas.setPoliticaExcedente(faixasOrigem.getPoliticaExcedente());
            faixas.setRegras(new ArrayList<>(faixasOrigem.getRegras()));
            faixas.setTipoFaixa(faixasOrigem.getTipoFaixa());
            faixas.setFaixasIniciais(faixasOrigem.getFaixasIniciais());
            faixas.setFaixas(faixasOrigem.getFaixas());
            faixas.setValores(faixasOrigem.getValores());
            faixas.setValorExcedente(faixasOrigem.getValorExcedente());
            faixas.setMinimo(faixasOrigem.getMinimo());
            copia.setFaixas(faixas);
        }
        return copia;
    }
}
