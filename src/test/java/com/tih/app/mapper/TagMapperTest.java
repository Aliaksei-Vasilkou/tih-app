package com.tih.app.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tih.app.dto.TagDto;
import com.tih.app.model.Language;
import com.tih.app.model.Tag;

class TagMapperTest {

    private static final String JAVA_LANGUAGE_NAME = "Java";
    private static final String JAVA_LANGUAGE_CODE = "java";
    private static final String TAG_NAME = "Concurrency";

    private final TagMapper mapper = new TagMapperImpl();

    @Test
    void toDto_mapsAllFieldsIncludingLanguage() {
        // given
        Language language = Language.builder().id(1L).name(JAVA_LANGUAGE_NAME).code(JAVA_LANGUAGE_CODE).build();
        Tag tag = Tag.builder().id(5L).name("GC").build();
        tag.setLanguage(language);

        // when
        TagDto result = mapper.toDto(tag);

        // then
        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getName()).isEqualTo("GC");
        assertThat(result.getLanguageId()).isEqualTo(1L);
        assertThat(result.getLanguageName()).isEqualTo(JAVA_LANGUAGE_NAME);
        assertThat(result.getLanguageCode()).isEqualTo(JAVA_LANGUAGE_CODE);
    }

    @Test
    void toDto_nullLanguage_returnsNullLanguageFields() {
        // given
        Tag tag = Tag.builder().id(1L).name(TAG_NAME).build();
        tag.setLanguage(null);

        // when
        TagDto result = mapper.toDto(tag);

        // then
        assertThat(result.getName()).isEqualTo(TAG_NAME);
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
        Language language = Language.builder().id(1L).name(JAVA_LANGUAGE_NAME).code(JAVA_LANGUAGE_CODE).build();
        Tag t1 = Tag.builder().id(1L).name("GC").build();
        t1.setLanguage(language);
        Tag t2 = Tag.builder().id(2L).name(TAG_NAME).build();
        t2.setLanguage(language);

        // when
        List<TagDto> result = mapper.toDtoList(List.of(t1, t2));

        // then
        assertThat(result).hasSize(2)
                .extracting(TagDto::getName)
                .containsExactly("GC", TAG_NAME);
    }

    @Test
    void toDtoList_nullInput_returnsNull() {
        assertThat(mapper.toDtoList(null)).isNull();
    }
}
