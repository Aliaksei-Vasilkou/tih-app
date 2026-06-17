package com.tih.app.controller;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tih.app.dto.BatchAnalyseResponse;
import com.tih.app.dto.BatchCommitRequest;
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
    public ResponseEntity<BatchUploadResponse> upload(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(batchUploadService.processUpload(file));
    }

    @PostMapping(value = "/analyse", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Analyse a JSON file and classify new items vs duplicates")
    @ApiResponse(responseCode = "200", description = "Analysis completed")
    @ApiResponse(responseCode = "400", description = "Malformed payload or missing required fields",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "422", description = "Unknown language code",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<BatchAnalyseResponse> analyse(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(batchUploadService.analyseUpload(file));
    }

    @PostMapping(value = "/commit", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Commit reviewed duplicate decisions and insert new items")
    @ApiResponse(responseCode = "200", description = "Commit completed")
    @ApiResponse(responseCode = "400", description = "Missing request payload or empty work set",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<BatchUploadResponse> commit(@RequestBody BatchCommitRequest request) {
        return ResponseEntity.ok(batchUploadService.commitUpload(request));
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
