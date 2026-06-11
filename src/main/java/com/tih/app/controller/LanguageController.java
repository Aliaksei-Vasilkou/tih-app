package com.tih.app.controller;

import com.tih.app.dto.LanguageCreateRequest;
import com.tih.app.dto.LanguageDto;
import com.tih.app.exception.ErrorResponse;
import com.tih.app.service.LanguageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/languages")
@RequiredArgsConstructor
@Tag(name = "Languages", description = "Manage programming languages")
@ApiResponse(responseCode = "500", description = "Internal server error",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class LanguageController {

    private final LanguageService languageService;

    @GetMapping
    @Operation(summary = "Get all active languages")
    public ResponseEntity<List<LanguageDto>> findAll() {
        return ResponseEntity.ok(languageService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get language by ID")
    @ApiResponse(responseCode = "404", description = "Language not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<LanguageDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(languageService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new language")
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Language with the same name or code already exists",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<LanguageDto> create(@Valid @RequestBody LanguageCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(languageService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing language")
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Language not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<LanguageDto> update(@PathVariable Long id, @Valid @RequestBody LanguageCreateRequest request) {
        return ResponseEntity.ok(languageService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a language")
    @ApiResponse(responseCode = "404", description = "Language not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        languageService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
