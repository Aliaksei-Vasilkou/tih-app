package com.tih.app.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tih.app.dto.CategoryCreateRequest;
import com.tih.app.dto.CategoryDto;
import com.tih.app.model.Category;
import com.tih.app.model.Language;

class CategoryMapperTest {

    private static final String JAVA_LANGUAGE_NAME = "Java";
    private static final String JAVA_LANGUAGE_CODE = "java";
    private static final String CORE_CATEGORY_NAME = "Core";
    private static final String DATABASE_CATEGORY_NAME = "Database";

    private final CategoryMapper mapper = new CategoryMapperImpl();

    @Test
    void toDto_mapsAllFieldsIncludingLanguage() {
        // given
        Language language = Language.builder().id(1L).name(JAVA_LANGUAGE_NAME).code(JAVA_LANGUAGE_CODE).build();
        Category category = Category.builder().id(2L).name(CORE_CATEGORY_NAME).build();
        category.setLanguage(language);

        // when
        CategoryDto result = mapper.toDto(category);

        // then
        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getName()).isEqualTo(CORE_CATEGORY_NAME);
        assertThat(result.getLanguageId()).isEqualTo(1L);
        assertThat(result.getLanguageName()).isEqualTo(JAVA_LANGUAGE_NAME);
        assertThat(result.getLanguageCode()).isEqualTo(JAVA_LANGUAGE_CODE);
    }

    @Test
    void toDto_nullLanguage_returnsNullLanguageFields() {
        // given
        Category category = Category.builder().id(1L).name(DATABASE_CATEGORY_NAME).build();
        category.setLanguage(null);

        // when
        CategoryDto result = mapper.toDto(category);

        // then
        assertThat(result.getName()).isEqualTo(DATABASE_CATEGORY_NAME);
        assertThat(result.getLanguageId()).isNull();
        assertThat(result.getLanguageName()).isNull();
        assertThat(result.getLanguageCode()).isNull();
    }

    @Test
    void toDto_nullInput_returnsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void toDtoList_mapsAllElements() {
        // given
        List<Category> categories = List.of(
                Category.builder().id(1L).name(CORE_CATEGORY_NAME).build(),
                Category.builder().id(2L).name(DATABASE_CATEGORY_NAME).build());

        // when
        List<CategoryDto> result = mapper.toDtoList(categories);

        // then
        assertThat(result).hasSize(2)
                .extracting(CategoryDto::getName)
                .containsExactly(CORE_CATEGORY_NAME, DATABASE_CATEGORY_NAME);
    }

    @Test
    void toEntity_mapsName() {
        // given
        CategoryCreateRequest request = new CategoryCreateRequest(CORE_CATEGORY_NAME, 1L);

        // when
        Category result = mapper.toEntity(request);

        // then
        assertThat(result.getName()).isEqualTo(CORE_CATEGORY_NAME);
    }

    @Test
    void toEntity_nullInput_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void updateEntity_updatesName() {
        // given
        Category category = Category.builder().name("Old").build();
        CategoryCreateRequest request = new CategoryCreateRequest("New", 1L);

        // when
        mapper.updateEntity(request, category);

        // then
        assertThat(category.getName()).isEqualTo("New");
    }

    @Test
    void updateEntity_nullRequest_doesNotModify() {
        // given
        Category category = Category.builder().name(CORE_CATEGORY_NAME).build();

        // when
        mapper.updateEntity(null, category);

        // then
        assertThat(category.getName()).isEqualTo(CORE_CATEGORY_NAME);
    }
}
