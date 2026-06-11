package com.tih.app.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tih.app.dto.QuestionCreateRequest;
import com.tih.app.dto.QuestionDto;
import com.tih.app.model.Category;
import com.tih.app.model.Language;
import com.tih.app.model.Question;
import com.tih.app.model.Tag;

class QuestionMapperTest {

    private static final String JAVA_LANGUAGE_NAME = "Java";
    private static final String JAVA_LANGUAGE_CODE = "java";
    private static final String CORE_CATEGORY_NAME = "Core";
    private static final String ANSWER_CONTENT = "Java Virtual Machine";

    private final QuestionMapper mapper = new QuestionMapperImpl();

    @Test
    void toDto_mapsAllFields() {
        // given
        Language language = Language.builder().id(1L).name(JAVA_LANGUAGE_NAME).code(JAVA_LANGUAGE_CODE).build();
        Category category = Category.builder().id(2L).name(CORE_CATEGORY_NAME).build();
        Tag tag = Tag.builder().id(3L).name("JVM").build();
        tag.setLanguage(language);
        Question question = Question.builder()
                .id(10L)
                .questionText("What is JVM?")
                .answerContent(ANSWER_CONTENT)
                .language(language)
                .category(category)
                .tags(List.of(tag))
                .build();

        // when
        QuestionDto result = mapper.toDto(question);

        // then
        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getQuestionText()).isEqualTo("What is JVM?");
        assertThat(result.getAnswerContent()).isEqualTo(ANSWER_CONTENT);
        assertThat(result.getLanguageId()).isEqualTo(1L);
        assertThat(result.getLanguageName()).isEqualTo(JAVA_LANGUAGE_NAME);
        assertThat(result.getLanguageCode()).isEqualTo(JAVA_LANGUAGE_CODE);
        assertThat(result.getCategoryId()).isEqualTo(2L);
        assertThat(result.getCategoryName()).isEqualTo(CORE_CATEGORY_NAME);
        assertThat(result.getTags()).containsExactly("JVM");
    }

    @Test
    void toDto_tagsAreSortedAlphabetically() {
        // given
        Language language = Language.builder().id(1L).name(JAVA_LANGUAGE_NAME).code(JAVA_LANGUAGE_CODE).build();
        Category category = Category.builder().id(1L).name(CORE_CATEGORY_NAME).build();
        Tag gc = Tag.builder().id(1L).name("GC").build();
        gc.setLanguage(language);
        Tag collections = Tag.builder().id(2L).name("Collections").build();
        collections.setLanguage(language);
        Tag concurrency = Tag.builder().id(3L).name("Concurrency").build();
        concurrency.setLanguage(language);
        Question question = Question.builder()
                .id(1L)
                .questionText("Q")
                .language(language)
                .category(category)
                .tags(List.of(gc, concurrency, collections))
                .build();

        // when
        QuestionDto result = mapper.toDto(question);

        // then
        assertThat(result.getTags()).containsExactly("Collections", "Concurrency", "GC");
    }

    @Test
    void toDto_nullTags_returnsEmptyList() {
        // given
        Language language = Language.builder().id(1L).name(JAVA_LANGUAGE_NAME).code(JAVA_LANGUAGE_CODE).build();
        Question question = Question.builder()
                .id(1L)
                .questionText("Q")
                .language(language)
                .category(Category.builder().id(1L).name("C").build())
                .tags(null)
                .build();

        // when
        QuestionDto result = mapper.toDto(question);

        // then
        assertThat(result.getTags()).isEmpty();
    }

    @Test
    void toDto_nullLanguage_returnsNullLanguageFields() {
        // given
        Question question = Question.builder()
                .id(1L)
                .questionText("Q")
                .language(null)
                .category(Category.builder().id(1L).name("C").build())
                .tags(List.of())
                .build();

        // when
        QuestionDto result = mapper.toDto(question);

        // then
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
        Category category = Category.builder().id(1L).name(CORE_CATEGORY_NAME).build();
        Question q1 = Question.builder().id(1L).questionText("Q1").language(language).category(category).tags(List.of()).build();
        Question q2 = Question.builder().id(2L).questionText("Q2").language(language).category(category).tags(List.of()).build();

        // when
        List<QuestionDto> result = mapper.toDtoList(List.of(q1, q2));

        // then
        assertThat(result).hasSize(2)
                .extracting(QuestionDto::getQuestionText)
                .containsExactly("Q1", "Q2");
    }

    @Test
    void toEntity_mapsTextFields() {
        // given
        QuestionCreateRequest request = QuestionCreateRequest.builder()
                .questionText("What is JVM?")
                .answerContent(ANSWER_CONTENT)
                .languageId(1L)
                .categoryId(2L)
                .build();

        // when
        Question result = mapper.toEntity(request);

        // then
        assertThat(result.getQuestionText()).isEqualTo("What is JVM?");
        assertThat(result.getAnswerContent()).isEqualTo(ANSWER_CONTENT);
    }

    @Test
    void toEntity_nullInput_returnsNull() {
        assertThat(mapper.toEntity(null)).isNull();
    }

    @Test
    void updateEntity_updatesTextFields() {
        // given
        Question question = Question.builder()
                .questionText("Old?")
                .answerContent("Old answer")
                .build();
        QuestionCreateRequest request = QuestionCreateRequest.builder()
                .questionText("New?")
                .answerContent("New answer")
                .languageId(1L)
                .categoryId(2L)
                .build();

        // when
        mapper.updateEntity(request, question);

        // then
        assertThat(question.getQuestionText()).isEqualTo("New?");
        assertThat(question.getAnswerContent()).isEqualTo("New answer");
    }
}
