package com.tih.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionCreateRequest {

    @NotBlank(message = "Question text is required")
    private String questionText;

    private String answerContent;

    @NotNull(message = "Language ID is required")
    private Long languageId;

    @NotNull(message = "Category ID is required")
    private Long categoryId;
}
