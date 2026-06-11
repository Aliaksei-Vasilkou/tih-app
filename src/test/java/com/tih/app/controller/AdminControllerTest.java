package com.tih.app.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.test.web.servlet.MockMvc;

import com.tih.app.model.Question;
import com.tih.app.repository.QuestionRepository;
import com.tih.app.service.QuestionIndexService;

@WebMvcTest(AdminController.class)
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QuestionRepository questionRepository;

    @MockBean
    private QuestionIndexService questionIndexService;

    @MockBean
    private ElasticsearchOperations elasticsearchOperations;

    @Test
    void reindex_indexDoesNotExist_createsAndIndexesAllQuestions() throws Exception {
        // given
        IndexOperations indexOps = mock(IndexOperations.class);

        when(elasticsearchOperations.indexOps(any(Class.class))).thenReturn(indexOps);
        when(indexOps.exists()).thenReturn(false);
        when(questionRepository.findAllForExport(null, null)).thenReturn(List.of());

        // when - then
        mockMvc.perform(post("/api/v1/admin/reindex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.indexed").value(0));
    }

    @Test
    void reindex_indexExists_deletesRecreatestAndIndexesAllQuestions() throws Exception {
        // given
        IndexOperations indexOps = mock(IndexOperations.class);
        List<Question> questions = List.of(
                Question.builder().id(1L).questionText("Q1").build(),
                Question.builder().id(2L).questionText("Q2").build());

        when(elasticsearchOperations.indexOps(any(Class.class))).thenReturn(indexOps);
        when(indexOps.exists()).thenReturn(true);
        when(questionRepository.findAllForExport(null, null)).thenReturn(questions);

        // when - then
        mockMvc.perform(post("/api/v1/admin/reindex"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.indexed").value(2));
    }
}
