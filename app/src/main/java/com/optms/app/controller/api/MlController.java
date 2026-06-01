package com.optms.app.controller.api;

import com.optms.app.authentication.CompanyApiKeyFilter;
import com.optms.app.dto.MlPredictRequest;
import com.optms.app.dto.MlPredictResponse;
import com.optms.app.dto.MlRetrainUploadResponse;
import com.optms.app.dto.MlTrainRequest;
import com.optms.app.service.MlPredictionService;
import com.optms.app.service.MlRetrainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
@Tag(name = "Previsões", description = "Previsão de prazo de entrega por modelo de Machine Learning")
public class MlController {

    private final MlPredictionService mlPredictionService;
    private final MlRetrainService mlRetrainService;

    @GetMapping("/retrain/jobs")
    public ResponseEntity<List<Map<String, Object>>> listTrainingJobs(HttpServletRequest request) {
        Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        if (companyId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(mlRetrainService.listTrainingJobs(companyId));
    }

    @GetMapping("/datasets")
    public ResponseEntity<List<Map<String, Object>>> listTrainingDatasets(HttpServletRequest request) {
        Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        if (companyId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(mlRetrainService.listTrainingDatasets(companyId));
    }

    @GetMapping("/datasets/{inputId}/records")
    public ResponseEntity<List<Map<String, Object>>> listTrainingDatasetRows(
            @PathVariable Long inputId,
            HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        if (companyId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(mlRetrainService.listTrainingDatasetRows(inputId, companyId));
    }

    @DeleteMapping("/datasets/{inputId}")
    public ResponseEntity<Void> deleteTrainingDataset(
            @PathVariable Long inputId,
            HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        if (companyId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        mlRetrainService.deleteTrainingDataset(inputId, companyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/models")
    public ResponseEntity<List<Map<String, Object>>> listModels(HttpServletRequest request) {
        Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        if (companyId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(mlRetrainService.listModels(companyId));
    }

    @PatchMapping("/models/{modelId}/activate")
    public ResponseEntity<Void> activateModel(
            @PathVariable Long modelId,
            HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        if (companyId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        mlRetrainService.activateModel(modelId, companyId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/predict")
    @Operation(
            summary = "Prever prazo de entrega",
            description = "Recebe os dados operacionais do frete, identifica a Company pela API key e encaminha a previsão para o serviço interno FastAPI.",
            parameters = @Parameter(
                    name = "X-API-KEY",
                    in = ParameterIn.HEADER,
                    required = true,
                    description = "API key interna da Company ou API key externa ativa associada a uma Company"
            )
    )
    @ApiResponse(
            responseCode = "200",
            description = "Previsão calculada com sucesso",
            content = @Content(schema = @Schema(implementation = MlPredictResponse.class))
    )
    @ApiResponse(responseCode = "401", description = "API key ausente ou inválida")
    @ApiResponse(responseCode = "502", description = "Falha ao comunicar com o serviço de ML")
    public ResponseEntity<MlPredictResponse> predict(
            @RequestBody MlPredictRequest payload,
            HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        if (companyId != null) {
            payload.setCompanyId(companyId);
        }
        return ResponseEntity.ok(mlPredictionService.predict(payload));
    }

    @PostMapping("/retrain/upload-xlsx")
    public ResponseEntity<MlRetrainUploadResponse> retrain(
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        if (companyId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(mlRetrainService.uploadTrainingDataset(file, companyId));
    }

    @PostMapping("/train")
    public ResponseEntity<MlRetrainUploadResponse> train(
            @RequestBody MlTrainRequest payload,
            HttpServletRequest request
    ) {
        Long companyId = (Long) request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        if (companyId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(mlRetrainService.trainFromDatasets(companyId, payload.getInputIds()));
    }
}
