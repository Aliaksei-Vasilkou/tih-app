package com.tih.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tih.app.dto.PageResponse;
import com.tih.app.dto.QuestionCreateRequest;
import com.tih.app.dto.QuestionDto;
import com.tih.app.exception.ResourceNotFoundException;
import com.tih.app.service.QuestionService;
import com.tih.app.util.ErrorCode;

@WebMvcTest(QuestionController.class)
class QuestionControllerTest {

    private static final long QUESTION_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QuestionService questionService;

    @Test
    void findAll_defaultParams_returnsPage() throws Exception {
        // given
        PageResponse<QuestionDto> page = buildPage(List.of(buildDto()));

        when(questionService.findAll(null, null, 0, 20)).thenReturn(page);

        // when - then
        mockMvc.perform(get("/api/v1/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(QUESTION_ID))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void findAll_withFilters_passesFiltersToService() throws Exception {
        // given
        PageResponse<QuestionDto> page = buildPage(List.of(buildDto()));

        when(questionService.findAll(1L, 2L, 0, 10)).thenReturn(page);

        // when - then
        mockMvc.perform(get("/api/v1/questions")
                        .param("languageId", "1")
                        .param("categoryId", "2")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void search_returnsPage() throws Exception {
        // given
        PageResponse<QuestionDto> page = buildPage(List.of(buildDto()));

        when(questionService.search(any())).thenReturn(page);

        // when - then
        mockMvc.perform(get("/api/v1/questions/search").param("q", "JVM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(QUESTION_ID));
    }

    @Test
    void findById_existingQuestion_returnsDto() throws Exception {
        // given
        when(questionService.findById(QUESTION_ID)).thenReturn(buildDto());

        // when - then
        mockMvc.perform(get("/api/v1/questions/{id}", QUESTION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(QUESTION_ID))
                .andExpect(jsonPath("$.questionText").value("What is JVM?"));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        // given
        when(questionService.findById(99L)).thenThrow(new ResourceNotFoundException("Question", 99L));

        // when - then
        mockMvc.perform(get("/api/v1/questions/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        // given
        QuestionCreateRequest request = QuestionCreateRequest.builder()
                .questionText("What is JVM?")
                .answerContent("Java Virtual Machine")
                .languageId(1L)
                .categoryId(2L)
                .build();

        when(questionService.create(request)).thenReturn(buildDto());

        // when - then
        mockMvc.perform(post("/api/v1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(QUESTION_ID));
    }

    @Test
    void create_missingRequiredField_returns400() throws Exception {
        // given
        QuestionCreateRequest request = QuestionCreateRequest.builder()
                .answerContent("Some answer")
                .languageId(1L)
                .categoryId(2L)
                .build();

        // when - then
        mockMvc.perform(post("/api/v1/questions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void update_existingQuestion_returnsUpdated() throws Exception {
        // given
        QuestionCreateRequest request = QuestionCreateRequest.builder()
                .questionText("Updated?")
                .answerContent("Updated answer")
                .languageId(1L)
                .categoryId(2L)
                .build();
        QuestionDto updated = QuestionDto.builder().id(QUESTION_ID).questionText("Updated?").build();

        when(questionService.update(eq(QUESTION_ID), any())).thenReturn(updated);

        // when - then
        mockMvc.perform(put("/api/v1/questions/{id}", QUESTION_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questionText").value("Updated?"));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        // given
        QuestionCreateRequest request = QuestionCreateRequest.builder()
                .questionText("Q?")
                .answerContent("A")
                .languageId(1L)
                .categoryId(2L)
                .build();

        when(questionService.update(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Question", 99L));

        // when - then
        mockMvc.perform(put("/api/v1/questions/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void delete_existingQuestion_returns204() throws Exception {
        // given
        doNothing().when(questionService).delete(QUESTION_ID);

        // when - then
        mockMvc.perform(delete("/api/v1/questions/{id}", QUESTION_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        // given
        doThrow(new ResourceNotFoundException("Question", 99L)).when(questionService).delete(99L);

        // when - then
        mockMvc.perform(delete("/api/v1/questions/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private QuestionDto buildDto() {
        return QuestionDto.builder()
                .id(QUESTION_ID)
                .questionText("What is JVM?")
                .answerContent("Java Virtual Machine")
                .build();
    }

    private PageResponse<QuestionDto> buildPage(List<QuestionDto> content) {
        return PageResponse.<QuestionDto>builder()
                .content(content)
                .page(0)
                .size(20)
                .totalElements(content.size())
                .totalPages(1)
                .last(true)
                .build();
    }
}
