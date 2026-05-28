package com.optms.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.optms.app.dto.CotacaoRequest;
import com.optms.app.dto.CotacaoResponse;
import com.optms.app.dto.TabelaFreteRequest;
import com.optms.app.model.ConfiguracaoCalculoFrete;
import com.optms.app.model.ObjetoFrete;
import com.optms.app.model.RegraCalculo;
import com.optms.app.model.TabelaFrete;
import com.optms.app.repository.ObjetoFreteRepository;
import com.optms.app.repository.TabelaFreteRepository;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class CotacaoServiceTest {

    private final RepositoryState repositoryState = new RepositoryState();
    private final TabelaFreteRepository tabelaFreteRepository = repositoryProxy(
            TabelaFreteRepository.class,
            (proxy, method, args) -> switch (method.getName()) {
                case "findByCompanyIdAndAtivaTrue" -> List.of(repositoryState.table);
                case "toString" -> "TabelaFreteRepositoryProxy";
                default -> throw new UnsupportedOperationException(method.getName());
            });
    private final ObjetoFreteRepository objetoFreteRepository = repositoryProxy(
            ObjetoFreteRepository.class,
            (proxy, method, args) -> switch (method.getName()) {
                case "findByTabelaId" -> repositoryState.objects;
                case "toString" -> "ObjetoFreteRepositoryProxy";
                default -> throw new UnsupportedOperationException(method.getName());
            });
    private final CotacaoService cotacaoService = new CotacaoService(tabelaFreteRepository, objetoFreteRepository);

    @Test
    void calculatesConstantFixedBaseFreight() {
        prepareQuotation(List.of("SP"), partida(constantRule("VALOR_NOTA", "VALOR_FIXO", 100.0)));

        CotacaoResponse response = cotacaoService.calcular(request("SP", "RJ", 12.0, 1000.0), 1L);

        assertEquals(100.0, response.getCotacoes().getFirst().freteBase());
        assertEquals(100.0, response.getCotacoes().getFirst().total());
    }

    @Test
    void calculatesWeightRange() {
        ObjetoFrete partida = partida(rangeConfig("PESO_BRUTO",
                rule(null, 10.0, "PESO_BRUTO", "VALOR_FIXO", 50.0),
                rule(10.0, null, "PESO_BRUTO", "VALOR_FIXO", 80.0)));
        prepareQuotation(List.of("SP"), partida);

        CotacaoResponse response = cotacaoService.calcular(request("SP", "RJ", 12.0, 1000.0), 1L);

        assertEquals(80.0, response.getCotacoes().getFirst().freteBase());
    }

    @Test
    void calculatesInvoiceValueRangeWithPercentage() {
        ObjetoFrete partida = partida(rangeConfig("VALOR_NOTA",
                rule(null, 1000.0, "VALOR_NOTA", "PERCENTUAL", 3.0),
                rule(1000.0, null, "VALOR_NOTA", "PERCENTUAL", 5.0)));
        prepareQuotation(List.of("SP"), partida);

        CotacaoResponse response = cotacaoService.calcular(request("SP", "RJ", 8.0, 2000.0), 1L);

        assertEquals(100.0, response.getCotacoes().getFirst().freteBase());
    }

    @Test
    void calculatesMultiplier() {
        prepareQuotation(List.of("SP"), partida(constantRule("PESO_BRUTO", "MULTIPLICADOR", 2.0)));

        CotacaoResponse response = cotacaoService.calcular(request("SP", "RJ", 10.0, 1000.0), 1L);

        assertEquals(20.0, response.getCotacoes().getFirst().freteBase());
    }

    @Test
    void calculatesComponentOverBaseFreight() {
        ObjetoFrete partida = partida(constantRule("VALOR_NOTA", "VALOR_FIXO", 100.0));
        ObjetoFrete component = component("GRIS", constantRule("VALOR_FRETE_PARTIDA", "PERCENTUAL", 10.0));
        prepareQuotation(List.of("SP"), partida, component);

        CotacaoResponse response = cotacaoService.calcular(request("SP", "RJ", 10.0, 1000.0), 1L);

        assertEquals(100.0, response.getCotacoes().getFirst().freteBase());
        assertEquals(10.0, response.getCotacoes().getFirst().componentes().getFirst().valor());
        assertEquals(110.0, response.getCotacoes().getFirst().total());
    }

    @Test
    void findsTableWhenOriginIsContainedInOriginList() {
        ObjetoFrete partida = partida(constantRule("VALOR_NOTA", "VALOR_FIXO", 100.0));
        partida.setUfOrigem("MG");
        prepareQuotation(List.of("SP", "MG"), partida);

        CotacaoResponse response = cotacaoService.calcular(request("MG", "RJ", 12.0, 1000.0), 1L);

        assertEquals("MG", response.getUfOrigem());
        assertEquals(100.0, response.getCotacoes().getFirst().total());
    }

    @Test
    void keepsRouteValuesSeparatedByOrigin() {
        ObjetoFrete spPartida = partida(constantRule("VALOR_NOTA", "VALOR_FIXO", 100.0));
        ObjetoFrete mgPartida = partida(constantRule("VALOR_NOTA", "VALOR_FIXO", 180.0));
        mgPartida.setUfOrigem("MG");
        prepareQuotation(List.of("SP", "MG"), spPartida, mgPartida);

        CotacaoResponse response = cotacaoService.calcular(request("MG", "RJ", 12.0, 1000.0), 1L);

        assertEquals(180.0, response.getCotacoes().getFirst().freteBase());
    }

    @Test
    void rejectsBaseFreightUnitOnPartidaObject() {
        TabelaFreteService service = new TabelaFreteService(null, null, null);
        TabelaFreteRequest request = new TabelaFreteRequest();
        request.setUfsOrigem(List.of("SP"));
        TabelaFreteRequest.ObjetoFreteDto object = new TabelaFreteRequest.ObjetoFreteDto();
        object.setTipoObjeto("PARTIDA");
        object.setUfOrigem("SP");
        object.setUfDestino("RJ");
        object.setConfigCalculo(constantRule("VALOR_FRETE_PARTIDA", "PERCENTUAL", 10.0));
        request.setObjetos(List.of(object));

        assertThrows(ResponseStatusException.class, () -> service.criar(request, 1L));
    }

    private void prepareQuotation(List<String> origins, ObjetoFrete... objects) {
        TabelaFrete table = new TabelaFrete();
        table.setId(10L);
        table.setCompanyId(1L);
        table.setNome("Tabela Teste");
        table.setAtiva(true);
        table.setUfsOrigem(origins);

        repositoryState.table = table;
        repositoryState.objects = List.of(objects);
    }

    private CotacaoRequest request(String origin, String destination, Double weight, Double invoiceValue) {
        CotacaoRequest request = new CotacaoRequest();
        request.setUfOrigem(origin);
        request.setUfDestino(destination);
        request.setPeso(weight);
        request.setValorNF(invoiceValue);
        return request;
    }

    private ObjetoFrete partida(ConfiguracaoCalculoFrete config) {
        ObjetoFrete object = new ObjetoFrete();
        object.setTipoObjeto("PARTIDA");
        object.setNomeComponente("FRETE_PARTIDA");
        object.setUfOrigem("SP");
        object.setUfDestino("RJ");
        object.setConfigCalculo(config);
        return object;
    }

    private ObjetoFrete component(String name, ConfiguracaoCalculoFrete config) {
        ObjetoFrete object = new ObjetoFrete();
        object.setTipoObjeto("COMPONENTE");
        object.setNomeComponente(name);
        object.setUfOrigem("SP");
        object.setUfDestino("RJ");
        object.setConfigCalculo(config);
        return object;
    }

    private ConfiguracaoCalculoFrete constantRule(String unidadeVariante, String tipoCalculo, Double valorCalculo) {
        ConfiguracaoCalculoFrete config = new ConfiguracaoCalculoFrete();
        config.setFormaCalculo("CONSTANTE");
        config.setRegras(List.of(rule(null, null, unidadeVariante, tipoCalculo, valorCalculo)));
        return config;
    }

    private ConfiguracaoCalculoFrete rangeConfig(String unidadeFaixa, RegraCalculo... regras) {
        ConfiguracaoCalculoFrete config = new ConfiguracaoCalculoFrete();
        config.setFormaCalculo("FAIXA");
        config.setUnidadeFaixa(unidadeFaixa);
        config.setRegras(List.of(regras));
        return config;
    }

    private RegraCalculo rule(
            Double limiteInicial,
            Double limiteFinal,
            String unidadeVariante,
            String tipoCalculo,
            Double valorCalculo
    ) {
        RegraCalculo regra = new RegraCalculo();
        regra.setLimiteInicial(limiteInicial);
        regra.setLimiteFinal(limiteFinal);
        regra.setUnidadeVariante(unidadeVariante);
        regra.setTipoCalculo(tipoCalculo);
        regra.setValorCalculo(valorCalculo);
        return regra;
    }

    @SuppressWarnings("unchecked")
    private <T> T repositoryProxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler);
    }

    private static class RepositoryState {
        private TabelaFrete table;
        private List<ObjetoFrete> objects = List.of();
    }
}
