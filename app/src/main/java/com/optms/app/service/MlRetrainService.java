package com.optms.app.service;

import com.optms.app.dto.MlRetrainUploadResponse;
import com.optms.app.util.TrainingExcelParser;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class MlRetrainService {

    private final TrainingExcelParser trainingExcelParser;
    private final RestTemplate restTemplate;
    private final String mlApiBaseUrl;

    public MlRetrainService(
            TrainingExcelParser trainingExcelParser,
            @Value("${ml.api.base-url}") String mlApiBaseUrl
    ) {
        this.trainingExcelParser = trainingExcelParser;
        this.mlApiBaseUrl = mlApiBaseUrl;
        this.restTemplate = new RestTemplate();
    }

    public MlRetrainUploadResponse retrainFromXlsx(MultipartFile file) {
        List<Map<String, Object>> records = trainingExcelParser.parse(file);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(Map.of("records", records), headers);

        try {
            MlRetrainUploadResponse response = restTemplate.postForObject(
                    mlApiBaseUrl + "/retrain/",
                    requestEntity,
                    MlRetrainUploadResponse.class
            );

            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "O serviço de ML retornou uma resposta vazia");
            }

            return response;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Falha ao comunicar com o serviço de ML", exception);
        }
    }
}
