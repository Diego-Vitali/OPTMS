package com.optms.app.service;

import com.optms.app.dto.TabelaFreteRequest;
import com.optms.app.dto.TabelaFreteRequest.ObjetoFreteDto;
import com.optms.app.model.ObjetoFrete;
import com.optms.app.model.TabelaFrete;
import com.optms.app.repository.ObjetoFreteRepository;
import com.optms.app.repository.TabelaFreteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** Gerencia o cadastro de tabelas de frete e seus componentes. */
@Service
@RequiredArgsConstructor
public class TabelaFreteService {

    private final TabelaFreteRepository tabelaFreteRepository;
    private final ObjetoFreteRepository objetoFreteRepository;

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
