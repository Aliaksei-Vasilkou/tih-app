package com.tih.app.controller;

import com.tih.app.dto.TagCreateRequest;
import com.tih.app.dto.TagDto;
import com.tih.app.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/languages/{languageId}/tags")
@RequiredArgsConstructor
@Tag(name = "Tags", description = "Manage tags per language")
public class TagController {

    private final TagService tagService;

    @GetMapping
    @Operation(summary = "List all tags for a language")
    public ResponseEntity<List<TagDto>> findAll(@PathVariable Long languageId) {
        return ResponseEntity.ok(tagService.findAllByLanguageId(languageId));
    }

    @PostMapping
    @Operation(summary = "Create a new tag for a language")
    public ResponseEntity<TagDto> create(
            @PathVariable Long languageId,
            @Valid @RequestBody TagCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.create(languageId, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing tag")
    public ResponseEntity<TagDto> update(
            @PathVariable Long languageId,
            @PathVariable Long id,
            @Valid @RequestBody TagCreateRequest request) {
        return ResponseEntity.ok(tagService.update(languageId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a tag")
    public ResponseEntity<Void> delete(
            @PathVariable Long languageId,
            @PathVariable Long id) {
        tagService.delete(languageId, id);
        return ResponseEntity.noContent().build();
    }
}
