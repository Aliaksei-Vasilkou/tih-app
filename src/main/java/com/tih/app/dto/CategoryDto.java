package com.tih.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    private Long id;
    private String name;
    private Long languageId;
    private String languageName;
    private String languageCode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
