package com.optms.app.service;

import com.optms.app.dto.TabelaFreteRequest;
import com.optms.app.dto.TabelaFreteRequest.ObjetoFreteDto;
import com.optms.app.dto.TabelaFreteUploadResponse;
import com.optms.app.model.ObjetoFrete;
import com.optms.app.model.TabelaFrete;
import com.optms.app.repository.ObjetoFreteRepository;
import com.optms.app.repository.TabelaFreteRepository;
import com.optms.app.util.FreightTableExcelParser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Gerencia o cadastro de tabelas de frete e seus componentes. */
@Service
@RequiredArgsConstructor
public class TabelaFreteService {

    private final TabelaFreteRepository tabelaFreteRepository;
    private final ObjetoFreteRepository objetoFreteRepository;
    private final FreightTableExcelParser freightTableExcelParser;

    /** Persiste a tabela de frete com todos os seus objetos (PARTIDA + COMPONENTEs). */
    @Transactional
    public TabelaFrete criar(TabelaFreteRequest request, Long companyId) {
        TabelaFrete tabela = new TabelaFrete();
        tabela.setCompanyId(companyId);
        tabela.setUfOrigem(request.getUfOrigem());
        tabela.setNome(request.getNome());
        tabela.setAtiva(request.isAtiva());
        tabela = tabelaFreteRepository.save(tabela);

        if (request.getObjetos() != null && !request.getObjetos().isEmpty()) {
            Long tabelaId = tabela.getId();
            List<ObjetoFrete> objetos = request.getObjetos().stream()
                    .map(dto -> toEntity(dto, tabelaId))
                    .toList();
            objetoFreteRepository.saveAll(objetos);
        }

        return tabela;
    }

    @Transactional
    public TabelaFreteUploadResponse criarPorXlsx(MultipartFile file, Long companyId) {
        TabelaFreteRequest request = freightTableExcelParser.parse(file);
        TabelaFrete tabela = criar(request, companyId);
        int objectsCreated = request.getObjetos() != null ? request.getObjetos().size() : 0;
        return new TabelaFreteUploadResponse(
                tabela.getId(),
                tabela.getNome(),
                tabela.getUfOrigem(),
                objectsCreated,
                "Tabela de frete criada com sucesso a partir do arquivo xlsx"
        );
    }

    public List<TabelaFrete> listar(Long companyId) {
        return tabelaFreteRepository.findByCompanyId(companyId);
    }

    public List<TabelaFrete> listarComoAdmin(Long companyId) {
        if (companyId != null) {
            return tabelaFreteRepository.findByCompanyId(companyId);
        }
        return tabelaFreteRepository.findAll();
    }

    @Transactional
    public TabelaFrete ativarPorId(Long id) {
        TabelaFrete tabela = tabelaFreteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tabela de frete não encontrada"));

        tabela.setAtiva(true);
        return tabelaFreteRepository.save(tabela);
    }

    @Transactional
    public TabelaFrete ativarPorIdECompany(Long id, Long companyId) {
        TabelaFrete tabela = tabelaFreteRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tabela de frete não encontrada"));

        tabela.setAtiva(true);
        return tabelaFreteRepository.save(tabela);
    }

    @Transactional
    public TabelaFrete desativarPorId(Long id) {
        TabelaFrete tabela = tabelaFreteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tabela de frete não encontrada"));

        tabela.setAtiva(false);
        return tabelaFreteRepository.save(tabela);
    }

    @Transactional
    public TabelaFrete desativarPorIdECompany(Long id, Long companyId) {
        TabelaFrete tabela = tabelaFreteRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tabela de frete não encontrada"));

        tabela.setAtiva(false);
        return tabelaFreteRepository.save(tabela);
    }

    private ObjetoFrete toEntity(ObjetoFreteDto dto, Long tabelaId) {
        ObjetoFrete obj = new ObjetoFrete();
        obj.setTabelaId(tabelaId);
        obj.setUf(dto.getUf());
        obj.setTipoObjeto(dto.getTipoObjeto());
        obj.setBaseCalculo(dto.getBaseCalculo());
        obj.setTipoCalculo(dto.getTipoCalculo());
        obj.setNomeComponente(dto.getNomeComponente());
        obj.setSobreFretePartida(dto.isSobreFretePartida());
        obj.setFaixas(dto.getFaixas());
        return obj;
    }
}
