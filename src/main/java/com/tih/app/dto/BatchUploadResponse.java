package com.tih.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadResponse {

    private int totalItems;
    private int successCount;
    private int failureCount;
    private int skippedCount;
    private List<String> errors;
    private List<String> skipped;
}
