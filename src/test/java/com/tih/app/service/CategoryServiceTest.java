package com.tih.app.service;

import com.tih.app.dto.CategoryCreateRequest;
import com.tih.app.dto.CategoryDto;
import com.tih.app.exception.DuplicateResourceException;
import com.tih.app.exception.ResourceNotFoundException;
import com.tih.app.mapper.CategoryMapper;
import com.tih.app.model.Category;
import com.tih.app.model.Language;
import com.tih.app.repository.CategoryRepository;
import com.tih.app.repository.LanguageRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final long CATEGORY_ID = 1L;
    private static final long LANGUAGE_ID = 1L;
    private static final long NON_EXISTENT_ID = 99L;
    private static final String DATABASE = "Database";
    private static final String DATABASE_UPDATED = "Database Updated";
    private static final String CORE = "Core";
    private static final String JAVA = "Java";

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private LanguageRepository languageRepository;
    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService service;

    @Test
    void shouldReturnAllCategories_whenRepositoryHasEntries() {
        // given
        Category db = buildCategory(CATEGORY_ID, DATABASE);
        Category core = buildCategory(2L, CORE);
        CategoryDto dbDto = buildCategoryDto(CATEGORY_ID, DATABASE);
        CategoryDto coreDto = buildCategoryDto(2L, CORE);

        when(categoryRepository.findAll()).thenReturn(List.of(db, core));
        when(categoryMapper.toDtoList(List.of(db, core))).thenReturn(List.of(dbDto, coreDto));

        // when
        List<CategoryDto> result = service.findAll();

        // then
        assertThat(result).hasSize(2)
                .extracting(CategoryDto::getName)
                .containsExactly(DATABASE, CORE);
    }

    @Test
    void shouldReturnEmptyList_whenNoCategoriesExist() {
        // given
        when(categoryRepository.findAll()).thenReturn(List.of());
        when(categoryMapper.toDtoList(List.of())).thenReturn(List.of());

        // when
        List<CategoryDto> result = service.findAll();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnCategoriesByLanguage_whenEntriesExist() {
        // given
        Category core = buildCategory(CATEGORY_ID, CORE);
        CategoryDto coreDto = buildCategoryDto(CATEGORY_ID, CORE);

        when(categoryRepository.findAllByLanguageId(LANGUAGE_ID)).thenReturn(List.of(core));
        when(categoryMapper.toDtoList(List.of(core))).thenReturn(List.of(coreDto));

        // when
        List<CategoryDto> result = service.findByLanguage(LANGUAGE_ID);

        // then
        assertThat(result).hasSize(1)
                .extracting(CategoryDto::getName)
                .containsExactly(CORE);
    }

    @Test
    void shouldReturnEmptyList_whenNoCategoriesForLanguage() {
        // given
        when(categoryRepository.findAllByLanguageId(NON_EXISTENT_ID)).thenReturn(List.of());
        when(categoryMapper.toDtoList(List.of())).thenReturn(List.of());

        // when
        List<CategoryDto> result = service.findByLanguage(NON_EXISTENT_ID);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnCategoryDto_whenCategoryExists() {
        // given
        Category category = buildCategory(CATEGORY_ID, DATABASE);
        CategoryDto dto = buildCategoryDto(CATEGORY_ID, DATABASE);

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(categoryMapper.toDto(category)).thenReturn(dto);

        // when
        CategoryDto result = service.findById(CATEGORY_ID);

        // then
        assertThat(result.getId()).isEqualTo(CATEGORY_ID);
        assertThat(result.getName()).isEqualTo(DATABASE);
    }

    @Test
    void shouldThrowResourceNotFoundException_whenCategoryNotFound() {
        // given
        when(categoryRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.findById(NON_EXISTENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTENT_ID));
    }

    @Test
    void shouldCreateCategory_whenNameIsUniqueForLanguage() {
        // given
        Language language = buildLanguage(LANGUAGE_ID, JAVA);
        CategoryCreateRequest request = buildRequest(CORE, LANGUAGE_ID);
        Category entity = buildCategory(3L, CORE);
        CategoryDto dto = buildCategoryDto(3L, CORE);

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(categoryRepository.existsByNameAndLanguageId(CORE, LANGUAGE_ID)).thenReturn(false);
        when(categoryMapper.toEntity(request)).thenReturn(entity);
        when(categoryRepository.save(entity)).thenReturn(entity);
        when(categoryMapper.toDto(entity)).thenReturn(dto);

        // when
        CategoryDto result = service.create(request);

        // then
        assertThat(result.getName()).isEqualTo(CORE);
        verify(categoryRepository).save(entity);
    }

    @Test
    void shouldThrowResourceNotFoundException_whenLanguageNotFoundOnCreate() {
        // given
        CategoryCreateRequest request = buildRequest(CORE, NON_EXISTENT_ID);

        when(languageRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTENT_ID));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void shouldThrowDuplicateResourceException_whenCategoryNameExistsForLanguage() {
        // given
        Language language = buildLanguage(LANGUAGE_ID, JAVA);
        CategoryCreateRequest request = buildRequest(CORE, LANGUAGE_ID);

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(categoryRepository.existsByNameAndLanguageId(CORE, LANGUAGE_ID)).thenReturn(true);

        // when - then
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(CORE);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void shouldUpdateCategory_whenCategoryAndLanguageExist() {
        // given
        Language language = buildLanguage(2L, "General");
        Category category = buildCategory(CATEGORY_ID, DATABASE);
        CategoryCreateRequest request = buildRequest(DATABASE_UPDATED, 2L);
        CategoryDto dto = buildCategoryDto(CATEGORY_ID, DATABASE_UPDATED);

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(languageRepository.findById(2L)).thenReturn(Optional.of(language));
        when(categoryRepository.save(category)).thenReturn(category);
        when(categoryMapper.toDto(category)).thenReturn(dto);

        // when
        CategoryDto result = service.update(CATEGORY_ID, request);

        // then
        assertThat(result.getName()).isEqualTo(DATABASE_UPDATED);
        verify(categoryMapper).updateEntity(request, category);
        verify(categoryRepository).save(category);
    }

    @Test
    void shouldThrowResourceNotFoundException_whenUpdatingNonExistentCategory() {
        // given
        when(categoryRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.update(NON_EXISTENT_ID, buildRequest("X", LANGUAGE_ID)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTENT_ID));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void shouldThrowResourceNotFoundException_whenLanguageNotFoundOnUpdate() {
        // given
        Category category = buildCategory(CATEGORY_ID, DATABASE);

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(languageRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.update(CATEGORY_ID, buildRequest("X", NON_EXISTENT_ID)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTENT_ID));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void shouldDeleteCategory_whenCategoryExists() {
        // given
        Category category = buildCategory(CATEGORY_ID, DATABASE);

        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));

        // when
        service.delete(CATEGORY_ID);

        // then
        verify(categoryRepository).deleteById(CATEGORY_ID);
    }

    @Test
    void shouldThrowResourceNotFoundException_whenDeletingNonExistentCategory() {
        // given
        when(categoryRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.delete(NON_EXISTENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTENT_ID));
        verify(categoryRepository, never()).deleteById(any());
    }

    private Category buildCategory(Long id, String name) {
        return Category.builder()
                .id(id)
                .name(name)
                .build();
    }

    private CategoryDto buildCategoryDto(Long id, String name) {
        return CategoryDto.builder()
                .id(id)
                .name(name)
                .build();
    }

    private Language buildLanguage(Long id, String name) {
        return Language.builder()
                .id(id)
                .name(name)
                .code(name.toLowerCase())
                .build();
    }

    private CategoryCreateRequest buildRequest(String name, Long languageId) {
        return CategoryCreateRequest.builder()
                .name(name)
                .languageId(languageId)
                .build();
    }
}
