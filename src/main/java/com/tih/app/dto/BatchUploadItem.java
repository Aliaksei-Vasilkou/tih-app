package com.tih.app.dto;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadItem {

    // Present on records that were previously exported; used for deduplication on import.
    private UUID externalId;

    @NotBlank(message = "Question text is required")
    private String questionText;

    private String answerContent;

    @NotBlank(message = "Language code is required")
    private String languageCode;

    @NotBlank(message = "Category name is required")
    private String categoryName;
}
