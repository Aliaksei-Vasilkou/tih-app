package com.tih.app.controller;

import com.tih.app.exception.ErrorResponse;
import com.tih.app.model.Question;
import com.tih.app.model.QuestionDocument;
import com.tih.app.repository.QuestionRepository;
import com.tih.app.service.QuestionIndexService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "Administrative operations")
@ApiResponse(responseCode = "500", description = "Internal server error",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class AdminController {

    private final QuestionRepository questionRepository;
    private final QuestionIndexService questionIndexService;
    private final ElasticsearchOperations elasticsearchOperations;

    @PostMapping("/reindex")
    @Operation(summary = "Recreate the Elasticsearch index and re-sync all questions from PostgreSQL")
    public ResponseEntity<Map<String, Object>> reindex() {
        log.info("Manual re-index triggered via admin endpoint");

        IndexOperations indexOps = elasticsearchOperations.indexOps(QuestionDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.createWithMapping();

        List<Question> questions = questionRepository.findAllForExport(null, null);
        questionIndexService.reindexAll(questions);

        return ResponseEntity.ok(Map.of(
                "status", "ok",
                "indexed", questions.size()
        ));
    }
}
