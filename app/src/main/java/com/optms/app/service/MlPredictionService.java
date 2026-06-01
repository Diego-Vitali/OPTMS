package com.optms.app.service;

import com.optms.app.dto.MlPredictRequest;
import com.optms.app.dto.MlPredictResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MlPredictionService {

    private final RestTemplate restTemplate;
    private final String mlApiBaseUrl;

    public MlPredictionService(@Value("${ml.api.base-url}") String mlApiBaseUrl) {
        this.mlApiBaseUrl = mlApiBaseUrl;
        this.restTemplate = new RestTemplate();
    }

    public MlPredictResponse predict(MlPredictRequest request, Long companyId) {
        if (companyId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "API key ausente ou inválida");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("company_id", companyId);
        payload.put("Peso total bruto", request.getPesoTotalBruto());
        payload.put("Metro cúbico", request.getMetroCubico());
        payload.put("Valor NF", request.getValorNF());
        payload.put("Volume NF", request.getVolumeNF());
        payload.put("Tipo de frete NF", request.getTipoFreteNF());
        payload.put("Via de transporte", request.getViaTransporte());
        payload.put("UF emitente NF", request.getUfEmitenteNF());
        payload.put("UF destinatário NF", request.getUfDestinatarioNF());

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);

        try {
            MlPredictResponse response = restTemplate.postForObject(
                    mlApiBaseUrl + "/predict/",
                    requestEntity,
                    MlPredictResponse.class
            );

            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "O serviço de ML retornou uma resposta vazia");
            }

            if (response.getError() != null && !response.getError().isBlank()) {
                if (isMissingActiveModel(response.getError())) {
                    throw new ResponseStatusException(HttpStatus.NOT_FOUND, response.getError());
                }
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, response.getError());
            }

            return response;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao comunicar com o serviço de ML", exception);
        }
    }

    private boolean isMissingActiveModel(String message) {
        String normalized = message.toLowerCase();
        return normalized.contains("nenhum modelo ativo")
                || normalized.contains("no active model");
    }
}
