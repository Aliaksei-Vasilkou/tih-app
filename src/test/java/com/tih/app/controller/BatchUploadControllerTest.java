package com.tih.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tih.app.dto.BatchAnalyseResponse;
import com.tih.app.dto.BatchCommitRequest;
import com.tih.app.dto.BatchUploadResponse;
import com.tih.app.dto.ConflictResolution;
import com.tih.app.dto.DuplicateConflict;
import com.tih.app.dto.QuestionDto;
import com.tih.app.dto.QuestionTransferItem;
import com.tih.app.exception.UnknownLanguageException;
import com.tih.app.service.BatchUploadService;

@WebMvcTest(BatchUploadController.class)
class BatchUploadControllerTest {

    private static final UUID DUPLICATE_EXT_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");

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
                .updatedCount(0)
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
                .andExpect(jsonPath("$.updatedCount").value(0))
                .andExpect(jsonPath("$.skippedCount").value(1))
                .andExpect(jsonPath("$.failureCount").value(0));
    }

    @Test
    void analyse_validFile_returnsNewItemsAndDuplicates() throws Exception {
        // given
        QuestionTransferItem newItem = QuestionTransferItem.builder()
                .question("What is polymorphism?")
                .language("java")
                .category("Core")
                .build();
        QuestionTransferItem incoming = QuestionTransferItem.builder()
                .extId(DUPLICATE_EXT_ID)
                .question("What is inheritance?")
                .language("java")
                .category("Core")
                .build();
        DuplicateConflict duplicate = new DuplicateConflict(
                DUPLICATE_EXT_ID,
                QuestionDto.builder().id(42L).questionText("What is inheritance?").build(),
                incoming);
        BatchAnalyseResponse response = new BatchAnalyseResponse(List.of(newItem), List.of(duplicate));

        MockMultipartFile file = new MockMultipartFile(
                "file", "questions.json", MediaType.APPLICATION_JSON_VALUE, "[]".getBytes());
        when(batchUploadService.analyseUpload(any())).thenReturn(response);

        // when - then
        mockMvc.perform(multipart("/api/v1/batch/analyse").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.newItems.length()").value(1))
                .andExpect(jsonPath("$.duplicates.length()").value(1))
                .andExpect(jsonPath("$.duplicates[0].extId").value(DUPLICATE_EXT_ID.toString()));
    }

    @Test
    void analyse_unknownLanguage_returns422() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "questions.json", MediaType.APPLICATION_JSON_VALUE, "[]".getBytes());
        when(batchUploadService.analyseUpload(any())).thenThrow(new UnknownLanguageException("kotlin"));

        // when - then
        mockMvc.perform(multipart("/api/v1/batch/analyse").file(file))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("UNKNOWN_LANGUAGE"));
    }

    @Test
    void analyse_invalidPayload_returns400() throws Exception {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file", "questions.json", MediaType.APPLICATION_JSON_VALUE, "[]".getBytes());
        when(batchUploadService.analyseUpload(any())).thenThrow(new IllegalArgumentException("Bad payload"));

        // when - then
        mockMvc.perform(multipart("/api/v1/batch/analyse").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void commit_validRequest_returnsSummary() throws Exception {
        // given
        BatchCommitRequest request = new BatchCommitRequest(
                List.of(),
                List.of(new ConflictResolution(DUPLICATE_EXT_ID, "skip")));
        BatchUploadResponse response = BatchUploadResponse.builder()
                .totalItems(1)
                .successCount(0)
                .updatedCount(0)
                .failureCount(0)
                .skippedCount(1)
                .errors(List.of())
                .skipped(List.of(DUPLICATE_EXT_ID.toString()))
                .build();
        when(batchUploadService.commitUpload(any())).thenReturn(response);

        // when - then
        mockMvc.perform(post("/api/v1/batch/commit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.updatedCount").value(0))
                .andExpect(jsonPath("$.skippedCount").value(1));
    }

    @Test
    void commit_emptyPayload_returns400() throws Exception {
        // given
        BatchCommitRequest request = new BatchCommitRequest(List.of(), List.of());
        when(batchUploadService.commitUpload(any())).thenThrow(new IllegalArgumentException("At least one item required"));

        // when - then
        mockMvc.perform(post("/api/v1/batch/commit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.errors[0].message").value("At least one item required"));
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
