package com.optms.app.controller.api;

import com.optms.app.dto.MlRetrainUploadResponse;
import com.optms.app.dto.MlTrainRequest;
import com.optms.app.service.MlRetrainService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/ml")
@RequiredArgsConstructor
public class AdminMlController {

    private final MlRetrainService mlRetrainService;

    @GetMapping("/retrain/jobs")
    public ResponseEntity<List<Map<String, Object>>> listTrainingJobs(
            @RequestParam(required = false) Long companyId
    ) {
        return ResponseEntity.ok(mlRetrainService.listTrainingJobs(companyId));
    }

    @GetMapping("/datasets")
    public ResponseEntity<List<Map<String, Object>>> listTrainingDatasets(
            @RequestParam(required = false) Long companyId
    ) {
        return ResponseEntity.ok(mlRetrainService.listTrainingDatasets(companyId));
    }

    @GetMapping("/datasets/{inputId}/records")
    public ResponseEntity<List<Map<String, Object>>> listTrainingDatasetRows(
            @PathVariable Long inputId,
            @RequestParam(required = false) Long companyId
    ) {
        return ResponseEntity.ok(mlRetrainService.listTrainingDatasetRows(inputId, companyId));
    }

    @DeleteMapping("/datasets/{inputId}")
    public ResponseEntity<Void> deleteTrainingDataset(
            @PathVariable Long inputId,
            @RequestParam(required = false) Long companyId
    ) {
        mlRetrainService.deleteTrainingDataset(inputId, companyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/models")
    public ResponseEntity<List<Map<String, Object>>> listModels(
            @RequestParam(required = false) Long companyId
    ) {
        return ResponseEntity.ok(mlRetrainService.listModels(companyId));
    }

    @PatchMapping("/models/{modelId}/activate")
    public ResponseEntity<Void> activateModel(@PathVariable Long modelId) {
        mlRetrainService.activateModel(modelId, null);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/retrain/upload-xlsx")
    public ResponseEntity<MlRetrainUploadResponse> retrain(
            @RequestPart("file") MultipartFile file,
            @RequestParam Long companyId
    ) {
        return ResponseEntity.ok(mlRetrainService.uploadTrainingDataset(file, companyId));
    }

    @PostMapping("/train")
    public ResponseEntity<MlRetrainUploadResponse> train(@RequestBody MlTrainRequest payload) {
        return ResponseEntity.ok(mlRetrainService.trainFromDatasets(payload.getCompanyId(), payload.getInputIds()));
    }
}
