package com.tih.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDto {

    private Long id;
    private String questionText;
    private String answerContent;
    private Long languageId;
    private String languageName;
    private String languageCode;
    private Long categoryId;
    private String categoryName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private List<String> tags = new ArrayList<>();
    // Relevance score from Elasticsearch — only populated for search results, null otherwise.
    private Float relevanceScore;
}
