package com.tih.app.controller;

import com.tih.app.dto.LanguageCreateRequest;
import com.tih.app.dto.LanguageDto;
import com.tih.app.service.LanguageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/languages")
@RequiredArgsConstructor
@Tag(name = "Languages", description = "Manage programming languages")
public class LanguageController {

    private final LanguageService languageService;

    @GetMapping
    @Operation(summary = "Get all active languages")
    public ResponseEntity<List<LanguageDto>> findAll() {
        return ResponseEntity.ok(languageService.findAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get language by ID")
    public ResponseEntity<LanguageDto> findById(@PathVariable Long id) {
        return ResponseEntity.ok(languageService.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new language")
    public ResponseEntity<LanguageDto> create(@Valid @RequestBody LanguageCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(languageService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing language")
    public ResponseEntity<LanguageDto> update(@PathVariable Long id,
                                              @Valid @RequestBody LanguageCreateRequest request) {
        return ResponseEntity.ok(languageService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a language")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        languageService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
