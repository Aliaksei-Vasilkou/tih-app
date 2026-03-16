package com.tih.app.controller;

import com.tih.app.dto.PageResponse;
import com.tih.app.dto.QuestionCreateRequest;
import com.tih.app.dto.QuestionDto;
import com.tih.app.dto.QuestionSearchRequest;
import com.tih.app.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/questions")
@RequiredArgsConstructor
@Tag(name = "Questions", description = "Manage interview questions and answers")
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    @Operation(summary = "List questions with optional language/category filters")
    public ResponseEntity<PageResponse<QuestionDto>> findAll(
            @RequestParam(required = false) Long languageId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(questionService.findAll(languageId, categoryId, page, size));
    }

    @GetMapping("/search")
    @Operation(summary = "Full-text search questions by keyword with optional filters")
    public ResponseEntity<PageResponse<QuestionDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long languageId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        QuestionSearchRequest request = QuestionSearchRequest.builder()
                .query(q)
                .languageId(languageId)
                .categoryId(categoryId)
                .page(page)
                .size(size)
                .build();
        return ResponseEntity.ok(questionService.search(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a question by ID")
    public ResponseEntity<QuestionDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(questionService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new question")
    public ResponseEntity<QuestionDto> create(@Valid @RequestBody QuestionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(questionService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing question")
    public ResponseEntity<QuestionDto> update(@PathVariable Long id,
                                              @Valid @RequestBody QuestionCreateRequest request) {
        return ResponseEntity.ok(questionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a question")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
