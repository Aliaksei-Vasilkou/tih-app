package com.tih.app.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tih.app.dto.LanguageCreateRequest;
import com.tih.app.dto.LanguageDto;
import com.tih.app.model.Language;

class LanguageMapperTest {

    public static final String JAVA_LANGUAGE_NAME = "Java";
    public static final String JAVA_LANGUAGE_CODE = "java";
    public static final String GO_LANGUAGE_NAME = "Go";
    public static final String GO_LANGUAGE_CODE = "go";
    private final LanguageMapper mapper = new LanguageMapperImpl();

    @Test
    void toDto_mapsAllFields() {
        // given
        Language language = Language.builder()
                .id(1L)
                .name(JAVA_LANGUAGE_NAME)
                .code(JAVA_LANGUAGE_CODE)
                .build();

        // when
        LanguageDto result = mapper.toDto(language);

        // then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo(JAVA_LANGUAGE_NAME);
        assertThat(result.getCode()).isEqualTo(JAVA_LANGUAGE_CODE);
    }

    @Test
    void toDto_nullInput_returnsNull() {
        assertThat(mapper.toDto(null)).isNull();
    }

    @Test
    void toDtoList_mapsAllElements() {
        // given
        List<Language> languages = List.of(
                Language.builder().id(1L).name(JAVA_LANGUAGE_NAME).code(JAVA_LANGUAGE_CODE).build(),
                Language.builder().id(2L).name(GO_LANGUAGE_NAME).code(GO_LANGUAGE_CODE).build());

        // when
        List<LanguageDto> result = mapper.toDtoList(languages);

        // then
        assertThat(result).hasSize(2)
                .extracting(LanguageDto::getCode)
                .containsExactly(JAVA_LANGUAGE_CODE, GO_LANGUAGE_CODE);
    }

    @Test
    void toDtoList_nullInput_returnsNull() {
        assertThat(mapper.toDtoList(null)).isNull();
    }

    @Test
    void toEntity_mapsNameAndCode() {
        // given
        LanguageCreateRequest request = new LanguageCreateRequest(JAVA_LANGUAGE_NAME, JAVA_LANGUAGE_CODE);

        // when
        Language result = mapper.toEntity(request);

        // then
        assertThat(result.getName()).isEqualTo(JAVA_LANGUAGE_NAME);
        assertThat(result.getCode()).isEqualTo(JAVA_LANGUAGE_CODE);
    }

    @Test
    void toEntity_nullInput_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void updateEntity_updatesNameAndCode() {
        // given
        Language language = Language.builder().name("Old").code("old").build();
        LanguageCreateRequest request = new LanguageCreateRequest("New", "new");

        // when
        mapper.updateEntity(request, language);

        // then
        assertThat(language.getName()).isEqualTo("New");
        assertThat(language.getCode()).isEqualTo("new");
    }

    @Test
    void updateEntity_nullRequest_doesNotModify() {
        // given
        Language language = Language.builder().name(JAVA_LANGUAGE_NAME).code(JAVA_LANGUAGE_CODE).build();

        // when
        mapper.updateEntity(null, language);

        // then
        assertThat(language.getName()).isEqualTo(JAVA_LANGUAGE_NAME);
        assertThat(language.getCode()).isEqualTo(JAVA_LANGUAGE_CODE);
    }
}
