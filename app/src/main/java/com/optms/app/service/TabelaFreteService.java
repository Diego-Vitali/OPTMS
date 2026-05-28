package com.optms.app.service;

import com.optms.app.dto.TabelaFreteRequest;
import com.optms.app.dto.TabelaFreteRequest.ObjetoFreteDto;
import com.optms.app.dto.TabelaFreteResponse;
import com.optms.app.dto.TabelaFreteResponse.ObjetoFreteResponse;
import com.optms.app.dto.TabelaFreteUploadResponse;
import com.optms.app.model.ConfiguracaoCalculoFrete;
import com.optms.app.model.ObjetoFrete;
import com.optms.app.model.RegraCalculo;
import com.optms.app.model.TabelaFrete;
import com.optms.app.repository.ObjetoFreteRepository;
import com.optms.app.repository.TabelaFreteRepository;
import com.optms.app.util.FreightTableExcelParser;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Gerencia o cadastro de tabelas de frete e seus componentes. */
@Service
@RequiredArgsConstructor
public class TabelaFreteService {

    private static final Set<String> TIPOS_OBJETO = Set.of("PARTIDA", "COMPONENTE");
    private static final Set<String> FORMAS_CALCULO = Set.of("FAIXA", "CONSTANTE");
    private static final Set<String> UNIDADES_FAIXA = Set.of("PESO_BRUTO", "VALOR_NOTA");
    private static final Set<String> UNIDADES_VARIANTE = Set.of("PESO_BRUTO", "VALOR_NOTA", "VALOR_FRETE_PARTIDA");
    private static final Set<String> TIPOS_CALCULO = Set.of("VALOR_FIXO", "PERCENTUAL", "MULTIPLICADOR");

    private final TabelaFreteRepository tabelaFreteRepository;
    private final ObjetoFreteRepository objetoFreteRepository;
    private final FreightTableExcelParser freightTableExcelParser;

    /** Persiste a tabela de frete com todos os seus objetos (PARTIDA + COMPONENTEs). */
    @Transactional
    public TabelaFreteResponse criar(TabelaFreteRequest request, Long companyId) {
        validateRequest(request);

        TabelaFrete tabela = new TabelaFrete();
        tabela.setCompanyId(companyId);
        tabela.setUfsOrigem(resolveTableOrigins(request));
        tabela.setNome(request.getNome());
        tabela.setVigenciaInicio(request.getVigenciaInicio());
        tabela.setVigenciaFim(request.getVigenciaFim());
        tabela.setAtiva(request.isAtiva());
        tabela = tabelaFreteRepository.save(tabela);

        if (request.getObjetos() != null && !request.getObjetos().isEmpty()) {
            Long tabelaId = tabela.getId();
            List<ObjetoFrete> objetos = request.getObjetos().stream()
                    .map(dto -> toEntity(dto, tabelaId))
                    .toList();
            objetoFreteRepository.saveAll(objetos);
        }

        return toResponse(tabela);
    }

    @Transactional
    public TabelaFreteUploadResponse criarPorXlsx(MultipartFile file, Long companyId) {
        TabelaFreteRequest request = freightTableExcelParser.parse(file);
        TabelaFreteResponse tabela = criar(request, companyId);
        int objectsCreated = request.getObjetos() != null ? request.getObjetos().size() : 0;
        return new TabelaFreteUploadResponse(
                tabela.id(),
                tabela.nome(),
                tabela.ufsOrigem(),
                objectsCreated,
                "Tabela de frete criada com sucesso a partir do arquivo xlsx"
        );
    }

    public List<TabelaFreteResponse> listar(Long companyId) {
        return tabelaFreteRepository.findByCompanyId(companyId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TabelaFreteResponse> listarComoAdmin(Long companyId) {
        if (companyId != null) {
            return listar(companyId);
        }
        return tabelaFreteRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TabelaFreteResponse ativarPorId(Long id) {
        TabelaFrete tabela = tabelaFreteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tabela de frete não encontrada"));

        tabela.setAtiva(true);
        return toResponse(tabelaFreteRepository.save(tabela));
    }

    @Transactional
    public TabelaFreteResponse ativarPorIdECompany(Long id, Long companyId) {
        TabelaFrete tabela = tabelaFreteRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tabela de frete não encontrada"));

        tabela.setAtiva(true);
        return toResponse(tabelaFreteRepository.save(tabela));
    }

    @Transactional
    public TabelaFreteResponse desativarPorId(Long id) {
        TabelaFrete tabela = tabelaFreteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tabela de frete não encontrada"));

        tabela.setAtiva(false);
        return toResponse(tabelaFreteRepository.save(tabela));
    }

    @Transactional
    public TabelaFreteResponse desativarPorIdECompany(Long id, Long companyId) {
        TabelaFrete tabela = tabelaFreteRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tabela de frete não encontrada"));

        tabela.setAtiva(false);
        return toResponse(tabelaFreteRepository.save(tabela));
    }

    private ObjetoFrete toEntity(ObjetoFreteDto dto, Long tabelaId) {
        ObjetoFrete obj = new ObjetoFrete();
        obj.setTabelaId(tabelaId);
        obj.setUfOrigem(normalizeUf(dto.getUfOrigem()));
        obj.setUfDestino(normalizeUf(dto.getUfDestino()));
        obj.setTipoObjeto(normalizeToken(dto.getTipoObjeto()));
        obj.setNomeComponente(dto.getNomeComponente());
        obj.setConfigCalculo(dto.getConfigCalculo());
        return obj;
    }

    private TabelaFreteResponse toResponse(TabelaFrete tabela) {
        List<ObjetoFrete> objetos = objetoFreteRepository.findByTabelaId(tabela.getId());
        List<ObjetoFreteResponse> objetoResponses = objetos.stream()
                .map(objeto -> new ObjetoFreteResponse(
                        objeto.getId(),
                        objeto.getTipoObjeto(),
                        objeto.getNomeComponente(),
                        objeto.getUfOrigem(),
                        objeto.getUfDestino(),
                        objeto.getConfigCalculo()
                ))
                .toList();
        List<String> ufsDestino = objetoResponses.stream()
                .map(ObjetoFreteResponse::ufDestino)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();

        return new TabelaFreteResponse(
                tabela.getId(),
                tabela.getCompanyId(),
                tabela.getNome(),
                tabela.getVigenciaInicio(),
                tabela.getVigenciaFim(),
                tabela.isAtiva(),
                tabela.getUfsOrigem(),
                ufsDestino,
                objetoResponses
        );
    }

    private void validateRequest(TabelaFreteRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payload da tabela de frete não informado");
        }
        if (resolveTableOrigins(request).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe ao menos uma UF de origem");
        }
        if (request.getObjetos() == null || request.getObjetos().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe ao menos um objeto de frete");
        }

        request.getObjetos().forEach(this::validateObject);
    }

    private void validateObject(ObjetoFreteDto dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Objeto de frete inválido");
        }

        String tipoObjeto = normalizeToken(dto.getTipoObjeto());
        if (!TIPOS_OBJETO.contains(tipoObjeto)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de objeto inválido: " + dto.getTipoObjeto());
        }
        if (normalizeUf(dto.getUfOrigem()).isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UF de origem obrigatória para " + tipoObjeto);
        }
        if (normalizeUf(dto.getUfDestino()).isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "UF de destino obrigatória para " + tipoObjeto);
        }

        ConfiguracaoCalculoFrete config = dto.getConfigCalculo();
        if (config == null || config.getRegras() == null || config.getRegras().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configuração de cálculo obrigatória para " + tipoObjeto);
        }

        String formaCalculo = normalizeToken(config.getFormaCalculo());
        if (!FORMAS_CALCULO.contains(formaCalculo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Forma de cálculo inválida: " + config.getFormaCalculo());
        }
        config.setFormaCalculo(formaCalculo);

        if ("FAIXA".equals(formaCalculo)) {
            String unidadeFaixa = normalizeToken(config.getUnidadeFaixa());
            if (!UNIDADES_FAIXA.contains(unidadeFaixa)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unidade de faixa inválida: " + config.getUnidadeFaixa());
            }
            config.setUnidadeFaixa(unidadeFaixa);
        } else if (config.getRegras().size() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cálculo constante deve possuir uma única regra");
        }

        config.getRegras().forEach(regra -> validateRule(regra, tipoObjeto));
    }

    private void validateRule(RegraCalculo regra, String tipoObjeto) {
        String unidadeVariante = normalizeToken(regra.getUnidadeVariante());
        if (!UNIDADES_VARIANTE.contains(unidadeVariante)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unidade variante inválida: " + regra.getUnidadeVariante());
        }
        if ("VALOR_FRETE_PARTIDA".equals(unidadeVariante) && !"COMPONENTE".equals(tipoObjeto)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "VALOR_FRETE_PARTIDA só pode ser usado em COMPONENTE");
        }

        String tipoCalculo = normalizeToken(regra.getTipoCalculo());
        if (!TIPOS_CALCULO.contains(tipoCalculo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de cálculo inválido: " + regra.getTipoCalculo());
        }
        if (regra.getValorCalculo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor de cálculo obrigatório");
        }
        if (regra.getLimiteInicial() != null && regra.getLimiteFinal() != null
                && regra.getLimiteInicial() > regra.getLimiteFinal()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Limite inicial maior que limite final");
        }

        regra.setUnidadeVariante(unidadeVariante);
        regra.setTipoCalculo(tipoCalculo);
    }

    private List<String> resolveTableOrigins(TabelaFreteRequest request) {
        List<String> requestOrigins = normalizeUfs(request.getUfsOrigem());
        if (!requestOrigins.isEmpty()) {
            return requestOrigins;
        }
        if (request.getObjetos() == null) {
            return List.of();
        }
        LinkedHashSet<String> origins = request.getObjetos().stream()
                .map(ObjetoFreteDto::getUfOrigem)
                .map(this::normalizeUf)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new ArrayList<>(origins);
    }

    private List<String> normalizeUfs(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        LinkedHashSet<String> normalized = values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new ArrayList<>(normalized);
    }

    private String normalizeToken(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeUf(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
