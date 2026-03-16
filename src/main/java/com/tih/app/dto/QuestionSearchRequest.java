package com.tih.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionSearchRequest {
    private String query;
    private Long languageId;
    private Long categoryId;
    private int page;
    private int size;
}
