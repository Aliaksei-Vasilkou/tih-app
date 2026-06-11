package com.tih.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tih.app.dto.BatchUploadResponse;
import com.tih.app.service.BatchUploadService;

@WebMvcTest(BatchUploadController.class)
class BatchUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BatchUploadService batchUploadService;

    @Test
    void upload_validFile_returnsUploadSummary() throws Exception {
        // given
        BatchUploadResponse response = BatchUploadResponse.builder()
                .totalItems(3)
                .successCount(2)
                .skippedCount(1)
                .failureCount(0)
                .build();

        when(batchUploadService.processUpload(any())).thenReturn(response);

        MockMultipartFile file = new MockMultipartFile(
                "file", "questions.json", MediaType.APPLICATION_JSON_VALUE, "[]".getBytes());

        // when - then
        mockMvc.perform(multipart("/api/v1/batch/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.successCount").value(2))
                .andExpect(jsonPath("$.skippedCount").value(1))
                .andExpect(jsonPath("$.failureCount").value(0));
    }

    @Test
    void export_noFilter_returnsJsonFileWithContentDisposition() throws Exception {
        // given
        when(batchUploadService.exportQuestions(null, null)).thenReturn(List.of());

        // when - then
        mockMvc.perform(get("/api/v1/batch/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"questions-export.json\""));
    }

    @Test
    void export_withFilters_passesFiltersToService() throws Exception {
        // given
        when(batchUploadService.exportQuestions("java", "Core")).thenReturn(List.of());

        // when - then
        mockMvc.perform(get("/api/v1/batch/export")
                        .param("languageCode", "java")
                        .param("categoryName", "Core"))
                .andExpect(status().isOk());
    }
}
