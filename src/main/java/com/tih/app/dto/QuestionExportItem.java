package com.tih.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO used for exporting question-answer records to JSON.
 * It is a superset of {@link BatchUploadItem} so that exported files
 * can be imported directly on another device without any modifications.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionExportItem {

    // Stable portable identifier — used for deduplication on re-import.
    private UUID externalId;
    private String questionText;
    private String answerContent;
    private String languageCode;
    private String categoryName;
}
