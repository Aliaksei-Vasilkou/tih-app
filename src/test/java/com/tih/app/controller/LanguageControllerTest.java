package com.tih.app.controller;

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
import com.tih.app.dto.LanguageCreateRequest;
import com.tih.app.dto.LanguageDto;
import com.tih.app.exception.DuplicateResourceException;
import com.tih.app.exception.ResourceNotFoundException;
import com.tih.app.service.LanguageService;
import com.tih.app.util.ErrorCode;

@WebMvcTest(LanguageController.class)
class LanguageControllerTest {

    private static final long LANGUAGE_ID = 1L;
    private static final String JAVA_LANGUAGE_NAME = "Java";
    private static final String JAVA_LANGUAGE_CODE = "java";
    private static final String GO_LANGUAGE_NAME = "Go";
    private static final String GO_LANGUAGE_CODE = "go";
    private static final String LANGUAGE_RESOURCE = "Language";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LanguageService languageService;

    @Test
    void findAll_returnsLanguageList() throws Exception {
        // given
        when(languageService.findAll()).thenReturn(List.of(buildDto()));

        // when - then
        mockMvc.perform(get("/api/v1/languages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(LANGUAGE_ID))
                .andExpect(jsonPath("$[0].code").value(JAVA_LANGUAGE_CODE));
    }

    @Test
    void findById_existingLanguage_returnsDto() throws Exception {
        // given
        when(languageService.findById(LANGUAGE_ID)).thenReturn(buildDto());

        // when - then
        mockMvc.perform(get("/api/v1/languages/{id}", LANGUAGE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(LANGUAGE_ID))
                .andExpect(jsonPath("$.code").value(JAVA_LANGUAGE_CODE));
    }

    @Test
    void findById_notFound_returns404WithErrorCode() throws Exception {
        // given
        when(languageService.findById(99L)).thenThrow(new ResourceNotFoundException(LANGUAGE_RESOURCE, 99L));

        // when - then
        mockMvc.perform(get("/api/v1/languages/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND))
                .andExpect(jsonPath("$.source").value("tih-app"));
    }

    @Test
    void create_validRequest_returns201WithDto() throws Exception {
        // given
        LanguageCreateRequest request = new LanguageCreateRequest(JAVA_LANGUAGE_NAME, JAVA_LANGUAGE_CODE);

        when(languageService.create(request)).thenReturn(buildDto());

        // when - then
        mockMvc.perform(post("/api/v1/languages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(JAVA_LANGUAGE_CODE));
    }

    @Test
    void create_blankName_returns400WithValidationCode() throws Exception {
        // given
        LanguageCreateRequest request = new LanguageCreateRequest("", JAVA_LANGUAGE_CODE);

        // when - then
        mockMvc.perform(post("/api/v1/languages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED))
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void create_duplicateCode_returns409WithErrorCode() throws Exception {
        // given
        LanguageCreateRequest request = new LanguageCreateRequest(JAVA_LANGUAGE_NAME, JAVA_LANGUAGE_CODE);

        when(languageService.create(request))
                .thenThrow(new DuplicateResourceException(LANGUAGE_RESOURCE, "code", JAVA_LANGUAGE_CODE));

        // when - then
        mockMvc.perform(post("/api/v1/languages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_RESOURCE));
    }

    @Test
    void update_existingLanguage_returnsUpdatedDto() throws Exception {
        // given
        LanguageCreateRequest request = new LanguageCreateRequest(GO_LANGUAGE_NAME, GO_LANGUAGE_CODE);
        LanguageDto dto = LanguageDto.builder().id(LANGUAGE_ID).name(GO_LANGUAGE_NAME).code(GO_LANGUAGE_CODE).build();

        when(languageService.update(LANGUAGE_ID, request)).thenReturn(dto);

        // when - then
        mockMvc.perform(put("/api/v1/languages/{id}", LANGUAGE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(GO_LANGUAGE_CODE));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        // given
        LanguageCreateRequest request = new LanguageCreateRequest(GO_LANGUAGE_NAME, GO_LANGUAGE_CODE);

        when(languageService.update(99L, request))
                .thenThrow(new ResourceNotFoundException(LANGUAGE_RESOURCE, 99L));

        // when - then
        mockMvc.perform(put("/api/v1/languages/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void delete_existingLanguage_returns204() throws Exception {
        // given
        doNothing().when(languageService).delete(LANGUAGE_ID);

        // when - then
        mockMvc.perform(delete("/api/v1/languages/{id}", LANGUAGE_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        // given
        doThrow(new ResourceNotFoundException(LANGUAGE_RESOURCE, 99L)).when(languageService).delete(99L);

        // when - then
        mockMvc.perform(delete("/api/v1/languages/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private LanguageDto buildDto() {
        return LanguageDto.builder()
                .id(LANGUAGE_ID)
                .name(JAVA_LANGUAGE_NAME)
                .code(JAVA_LANGUAGE_CODE)
                .build();
    }
}
