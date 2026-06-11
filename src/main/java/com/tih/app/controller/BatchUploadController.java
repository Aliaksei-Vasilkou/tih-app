package com.tih.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tih.app.dto.BatchUploadResponse;
import com.tih.app.dto.QuestionTransferItem;
import com.tih.app.exception.ErrorResponse;
import com.tih.app.service.BatchUploadService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
@Tag(name = "Batch Upload", description = "Bulk import and export of questions and answers")
@ApiResponse(responseCode = "500", description = "Internal server error",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class BatchUploadController {

    private final BatchUploadService batchUploadService;
    private final ObjectMapper objectMapper;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload a JSON file containing an array of question-answer pairs")
    public ResponseEntity<BatchUploadResponse> upload(
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(batchUploadService.processUpload(file));
    }

    @GetMapping("/export")
    @Operation(summary = "Export all active questions to a re-importable JSON file",
            description = "Optionally filter by languageCode and/or categoryName. " +
                    "The returned file can be imported directly via the /upload endpoint.")
    public ResponseEntity<byte[]> export(
            @Parameter(description = "Filter by language code, e.g. 'java'")
            @RequestParam(required = false) String languageCode,
            @Parameter(description = "Filter by category name, e.g. 'Concurrency'")
            @RequestParam(required = false) String categoryName) throws Exception {

        List<QuestionTransferItem> items = batchUploadService.exportQuestions(languageCode, categoryName);
        byte[] body = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(items);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("questions-export.json").build());

        return ResponseEntity.ok().headers(headers).body(body);
    }
}
