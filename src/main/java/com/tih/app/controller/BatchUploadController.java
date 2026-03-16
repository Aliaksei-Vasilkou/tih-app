package com.tih.app.controller;

import com.tih.app.dto.BatchUploadResponse;
import com.tih.app.service.BatchUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
@Tag(name = "Batch Upload", description = "Bulk import of questions and answers")
public class BatchUploadController {

    private final BatchUploadService batchUploadService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a JSON file containing an array of question-answer pairs")
    public ResponseEntity<BatchUploadResponse> upload(
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(batchUploadService.processUpload(file));
    }
}
