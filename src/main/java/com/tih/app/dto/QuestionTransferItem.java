package com.tih.app.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Unified DTO for both import (batch upload) and export of questions.
 * <p>
 * JSON field names use the short form defined in the transfer format:
 * {@code extId}, {@code question}, {@code answer}, {@code language}, {@code category}, {@code tags}.
 * <p>
 * For backward compatibility, the old camelCase names are accepted as aliases on import.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionTransferItem {

    // Stable portable identifier — used for deduplication on re-import. Required on export, optional on import.
    private UUID extId;

    @NotBlank(message = "Question text is required")
    private String question;

    private String answer;

    @NotBlank(message = "Language code is required")
    private String language;

    @NotBlank(message = "Category name is required")
    private String category;

    // Optional set of tag names scoped to the question's language. Auto-created on import if missing.
    @JsonProperty("tags")
    @Builder.Default
    private List<String> tags = new ArrayList<>();
}
