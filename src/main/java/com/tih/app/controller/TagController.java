package com.tih.app.controller;

import com.tih.app.dto.TagCreateRequest;
import com.tih.app.dto.TagDto;
import com.tih.app.exception.ErrorResponse;
import com.tih.app.service.TagService;

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
@RequestMapping("/api/v1/languages/{languageId}/tags")
@RequiredArgsConstructor
@Tag(name = "Tags", description = "Manage tags per language")
@ApiResponse(responseCode = "500", description = "Internal server error",
        content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
public class TagController {

    private final TagService tagService;

    @GetMapping
    @Operation(summary = "List all tags for a language")
    @ApiResponse(responseCode = "404", description = "Language not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<TagDto>> findAll(@PathVariable Long languageId) {
        return ResponseEntity.ok(tagService.findAllByLanguageId(languageId));
    }

    @PostMapping
    @Operation(summary = "Create a new tag for a language")
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Language not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Tag with this name already exists for the language",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TagDto> create(
            @PathVariable Long languageId,
            @Valid @RequestBody TagCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tagService.create(languageId, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing tag")
    @ApiResponse(responseCode = "400", description = "Validation failed",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Language or tag not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Tag with this name already exists for the language",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TagDto> update(
            @PathVariable Long languageId,
            @PathVariable Long id,
            @Valid @RequestBody TagCreateRequest request) {
        return ResponseEntity.ok(tagService.update(languageId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a tag")
    @ApiResponse(responseCode = "404", description = "Language or tag not found",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> delete(
            @PathVariable Long languageId,
            @PathVariable Long id) {
        tagService.delete(languageId, id);
        
        return ResponseEntity.noContent().build();
    }
}
