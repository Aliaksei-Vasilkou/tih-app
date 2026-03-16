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
public class BatchUploadItem {

    @NotBlank(message = "Question text is required")
    private String questionText;

    private String answerContent;

    @NotBlank(message = "Language code is required")
    private String languageCode;

    @NotBlank(message = "Category name is required")
    private String categoryName;
}
