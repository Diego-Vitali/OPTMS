package com.optms.app.controller.api;

import com.optms.app.authentication.CompanyApiKeyFilter;
import com.optms.app.dto.MlPredictRequest;
import com.optms.app.dto.MlPredictResponse;
import com.optms.app.dto.MlRetrainUploadResponse;
import com.optms.app.service.MlPredictionService;
import com.optms.app.service.MlRetrainService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ml")
@RequiredArgsConstructor
public class MlController {

    private final MlPredictionService mlPredictionService;
    private final MlRetrainService mlRetrainService;

    @PostMapping("/predict")
    public ResponseEntity<MlPredictResponse> predict(
            @RequestBody MlPredictRequest payload,
            HttpServletRequest request
    ) {
        request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        return ResponseEntity.ok(mlPredictionService.predict(payload));
    }

    @PostMapping("/retrain/upload-xlsx")
    public ResponseEntity<MlRetrainUploadResponse> retrain(
            @RequestPart("file") MultipartFile file,
            HttpServletRequest request
    ) {
        request.getAttribute(CompanyApiKeyFilter.COMPANY_ID_ATTRIBUTE);
        return ResponseEntity.ok(mlRetrainService.retrainFromXlsx(file));
    }
}
