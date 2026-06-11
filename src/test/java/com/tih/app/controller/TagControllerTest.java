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
import com.tih.app.dto.TagCreateRequest;
import com.tih.app.dto.TagDto;
import com.tih.app.exception.DuplicateResourceException;
import com.tih.app.exception.ResourceNotFoundException;
import com.tih.app.service.TagService;
import com.tih.app.util.ErrorCode;

@WebMvcTest(TagController.class)
class TagControllerTest {

    private static final long LANGUAGE_ID = 1L;
    private static final long NON_EXISTENT_LANGUAGE_ID = 99L;
    private static final long TAG_ID = 10L;
    private static final String TAG_NAME = "GC";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TagService tagService;

    @Test
    void findAll_existingLanguage_returnsTagList() throws Exception {
        // given
        when(tagService.findAllByLanguageId(LANGUAGE_ID)).thenReturn(List.of(buildDto()));

        // when - then
        mockMvc.perform(get("/api/v1/languages/{languageId}/tags", LANGUAGE_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(TAG_ID))
                .andExpect(jsonPath("$[0].name").value(TAG_NAME));
    }

    @Test
    void findAll_languageNotFound_returns404() throws Exception {
        // given
        when(tagService.findAllByLanguageId(NON_EXISTENT_LANGUAGE_ID))
                .thenThrow(new ResourceNotFoundException("Language", NON_EXISTENT_LANGUAGE_ID));

        // when - then
        mockMvc.perform(get("/api/v1/languages/{languageId}/tags", NON_EXISTENT_LANGUAGE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        // given
        TagCreateRequest request = new TagCreateRequest(TAG_NAME);

        when(tagService.create(LANGUAGE_ID, request)).thenReturn(buildDto());

        // when - then
        mockMvc.perform(post("/api/v1/languages/{languageId}/tags", LANGUAGE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(TAG_NAME));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        // given
        TagCreateRequest request = new TagCreateRequest("");

        // when - then
        mockMvc.perform(post("/api/v1/languages/{languageId}/tags", LANGUAGE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void create_duplicate_returns409() throws Exception {
        // given
        TagCreateRequest request = new TagCreateRequest(TAG_NAME);

        when(tagService.create(LANGUAGE_ID, request))
                .thenThrow(new DuplicateResourceException("Tag", "name", TAG_NAME));

        // when - then
        mockMvc.perform(post("/api/v1/languages/{languageId}/tags", LANGUAGE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_RESOURCE));
    }

    @Test
    void update_existingTag_returnsUpdated() throws Exception {
        // given
        TagCreateRequest request = new TagCreateRequest("Garbage Collection");
        TagDto updated = TagDto.builder().id(TAG_ID).name("Garbage Collection").build();

        when(tagService.update(LANGUAGE_ID, TAG_ID, request)).thenReturn(updated);

        // when - then
        mockMvc.perform(put("/api/v1/languages/{languageId}/tags/{id}", LANGUAGE_ID, TAG_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Garbage Collection"));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        // given
        TagCreateRequest request = new TagCreateRequest(TAG_NAME);

        when(tagService.update(LANGUAGE_ID, NON_EXISTENT_LANGUAGE_ID, request))
                .thenThrow(new ResourceNotFoundException("Tag", NON_EXISTENT_LANGUAGE_ID));

        // when - then
        mockMvc.perform(put("/api/v1/languages/{languageId}/tags/{id}", LANGUAGE_ID, NON_EXISTENT_LANGUAGE_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void delete_existingTag_returns204() throws Exception {
        // given
        doNothing().when(tagService).delete(LANGUAGE_ID, TAG_ID);

        // when - then
        mockMvc.perform(delete("/api/v1/languages/{languageId}/tags/{id}", LANGUAGE_ID, TAG_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        // given
        doThrow(new ResourceNotFoundException("Tag", NON_EXISTENT_LANGUAGE_ID)).when(tagService).delete(LANGUAGE_ID, NON_EXISTENT_LANGUAGE_ID);

        // when - then
        mockMvc.perform(delete("/api/v1/languages/{languageId}/tags/{id}", LANGUAGE_ID, NON_EXISTENT_LANGUAGE_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private TagDto buildDto() {
        return TagDto.builder()
                .id(TAG_ID)
                .name(TAG_NAME)
                .languageId(LANGUAGE_ID)
                .build();
    }
}
