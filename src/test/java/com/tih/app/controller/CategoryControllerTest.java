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
import com.tih.app.dto.CategoryCreateRequest;
import com.tih.app.dto.CategoryDto;
import com.tih.app.exception.DuplicateResourceException;
import com.tih.app.exception.ResourceNotFoundException;
import com.tih.app.service.CategoryService;
import com.tih.app.util.ErrorCode;

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    private static final long CATEGORY_ID = 1L;
    private static final long LANGUAGE_ID = 1L;
    private static final String DATABASE = "Database";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Test
    void findAll_noFilter_returnsAllCategories() throws Exception {
        // given
        when(categoryService.findAll()).thenReturn(List.of(buildDto()));

        // when - then
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(CATEGORY_ID))
                .andExpect(jsonPath("$[0].name").value(DATABASE));
    }

    @Test
    void findAll_withLanguageFilter_returnsCategoriesByLanguage() throws Exception {
        // given
        when(categoryService.findByLanguage(LANGUAGE_ID)).thenReturn(List.of(buildDto()));

        // when - then
        mockMvc.perform(get("/api/v1/categories").param("languageId", String.valueOf(LANGUAGE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value(DATABASE));
    }

    @Test
    void findById_existingCategory_returnsDto() throws Exception {
        // given
        when(categoryService.findById(CATEGORY_ID)).thenReturn(buildDto());

        // when - then
        mockMvc.perform(get("/api/v1/categories/{id}", CATEGORY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CATEGORY_ID))
                .andExpect(jsonPath("$.name").value(DATABASE));
    }

    @Test
    void findById_notFound_returns404() throws Exception {
        // given
        when(categoryService.findById(99L)).thenThrow(new ResourceNotFoundException("Category", 99L));

        // when - then
        mockMvc.perform(get("/api/v1/categories/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        // given
        CategoryCreateRequest request = new CategoryCreateRequest(DATABASE, LANGUAGE_ID);

        when(categoryService.create(request)).thenReturn(buildDto());

        // when - then
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(DATABASE));
    }

    @Test
    void create_blankName_returns400() throws Exception {
        // given
        CategoryCreateRequest request = new CategoryCreateRequest("", LANGUAGE_ID);

        // when - then
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.VALIDATION_FAILED));
    }

    @Test
    void create_duplicate_returns409() throws Exception {
        // given
        CategoryCreateRequest request = new CategoryCreateRequest(DATABASE, LANGUAGE_ID);

        when(categoryService.create(request))
                .thenThrow(new DuplicateResourceException("Category", "name", DATABASE));

        // when - then
        mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.DUPLICATE_RESOURCE));
    }

    @Test
    void update_existingCategory_returnsUpdated() throws Exception {
        // given
        CategoryCreateRequest request = new CategoryCreateRequest("Database Updated", LANGUAGE_ID);
        CategoryDto updated = CategoryDto.builder().id(CATEGORY_ID).name("Database Updated").build();

        when(categoryService.update(CATEGORY_ID, request)).thenReturn(updated);

        // when - then
        mockMvc.perform(put("/api/v1/categories/{id}", CATEGORY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Database Updated"));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        // given
        CategoryCreateRequest request = new CategoryCreateRequest(DATABASE, LANGUAGE_ID);

        when(categoryService.update(99L, request))
                .thenThrow(new ResourceNotFoundException("Category", 99L));

        // when - then
        mockMvc.perform(put("/api/v1/categories/{id}", 99L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void delete_existingCategory_returns204() throws Exception {
        // given
        doNothing().when(categoryService).delete(CATEGORY_ID);

        // when - then
        mockMvc.perform(delete("/api/v1/categories/{id}", CATEGORY_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        // given
        doThrow(new ResourceNotFoundException("Category", 99L)).when(categoryService).delete(99L);

        // when - then
        mockMvc.perform(delete("/api/v1/categories/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private CategoryDto buildDto() {
        return CategoryDto.builder()
                .id(CATEGORY_ID)
                .name(DATABASE)
                .languageId(LANGUAGE_ID)
                .build();
    }
}
