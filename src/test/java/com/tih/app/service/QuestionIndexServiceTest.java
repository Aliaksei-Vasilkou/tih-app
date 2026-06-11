package com.tih.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import com.tih.app.model.Category;
import com.tih.app.model.Language;
import com.tih.app.model.Question;
import com.tih.app.model.QuestionDocument;
import com.tih.app.model.Tag;

@ExtendWith(MockitoExtension.class)
class QuestionIndexServiceTest {

    private static final long LANGUAGE_ID = 2L;
    private static final long CATEGORY_ID = 10L;
    private static final String LANGUAGE_CODE = "java";
    private static final String CATEGORY_NAME = "Core";
    private static final String QUESTION_TEXT = "What is JVM?";
    private static final String LEVEL_TAG = "L1";

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private QuestionIndexService service;

    @Test
    void shouldIndexQuestionDocument_whenQuestionIsValid() {
        // given
        Language language = buildLanguage();
        Category category = buildCategory();
        UUID extId = UUID.randomUUID();
        Question question = Question.builder()
                .id(1L)
                .externalId(extId)
                .questionText(QUESTION_TEXT)
                .answerContent("JVM is...")
                .language(language)
                .category(category)
                .tags(List.of(Tag.builder().id(5L).name(LEVEL_TAG).build()))
                .build();

        // when
        service.index(question);

        // then
        ArgumentCaptor<QuestionDocument> captor = ArgumentCaptor.forClass(QuestionDocument.class);
        verify(elasticsearchOperations).save(captor.capture());

        QuestionDocument doc = captor.getValue();
        assertThat(doc.getId()).isEqualTo("1");
        assertThat(doc.getExternalId()).isEqualTo(extId.toString());
        assertThat(doc.getQuestionText()).isEqualTo(QUESTION_TEXT);
        assertThat(doc.getLanguageCode()).isEqualTo(LANGUAGE_CODE);
        assertThat(doc.getCategoryName()).isEqualTo(CATEGORY_NAME);
        assertThat(doc.getTags()).containsExactly(LEVEL_TAG);
    }

    @Test
    void shouldIndexDocumentWithEmptyTags_whenQuestionHasNoTags() {
        // given
        Question question = Question.builder()
                .id(2L)
                .externalId(UUID.randomUUID())
                .questionText("What is GC?")
                .language(buildLanguage())
                .category(buildCategory())
                .tags(List.of())
                .build();

        // when
        service.index(question);

        // then
        ArgumentCaptor<QuestionDocument> captor = ArgumentCaptor.forClass(QuestionDocument.class);
        verify(elasticsearchOperations).save(captor.capture());

        assertThat(captor.getValue().getTags()).isEmpty();
    }

    @Test
    void shouldIndexDocumentWithEmptyTags_whenTagListIsNull() {
        // given
        Question question = Question.builder()
                .id(3L)
                .externalId(UUID.randomUUID())
                .questionText("What is heap?")
                .language(buildLanguage())
                .category(buildCategory())
                .tags(null)
                .build();

        // when
        service.index(question);

        // then
        ArgumentCaptor<QuestionDocument> captor = ArgumentCaptor.forClass(QuestionDocument.class);
        verify(elasticsearchOperations).save(captor.capture());

        assertThat(captor.getValue().getTags()).isEmpty();
    }

    @Test
    void shouldSetNullLanguageFields_whenQuestionHasNoLanguage() {
        // given
        Question question = Question.builder()
                .id(4L)
                .questionText(QUESTION_TEXT)
                .language(null)
                .category(buildCategory())
                .tags(List.of())
                .build();

        // when
        service.index(question);

        // then
        ArgumentCaptor<QuestionDocument> captor = ArgumentCaptor.forClass(QuestionDocument.class);
        verify(elasticsearchOperations).save(captor.capture());

        QuestionDocument doc = captor.getValue();
        assertThat(doc.getLanguageId()).isNull();
        assertThat(doc.getLanguageCode()).isNull();
        assertThat(doc.getLanguageName()).isNull();
    }

    @Test
    void shouldSetNullCategoryFields_whenQuestionHasNoCategory() {
        // given
        Question question = Question.builder()
                .id(5L)
                .questionText(QUESTION_TEXT)
                .language(buildLanguage())
                .category(null)
                .tags(List.of())
                .build();

        // when
        service.index(question);

        // then
        ArgumentCaptor<QuestionDocument> captor = ArgumentCaptor.forClass(QuestionDocument.class);
        verify(elasticsearchOperations).save(captor.capture());

        QuestionDocument doc = captor.getValue();
        assertThat(doc.getCategoryId()).isNull();
        assertThat(doc.getCategoryName()).isNull();
    }

    @Test
    void shouldDeleteDocumentByStringId_whenDeleteCalled() {
        // when
        service.delete(42L);

        // then
        verify(elasticsearchOperations).delete("42", QuestionDocument.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSaveAllDocuments_whenReindexAllCalled() {
        // given
        Question q1 = Question.builder()
                .id(1L)
                .externalId(UUID.randomUUID())
                .questionText("Q1")
                .language(buildLanguage())
                .category(buildCategory())
                .tags(List.of())
                .build();
        Question q2 = Question.builder()
                .id(2L)
                .externalId(UUID.randomUUID())
                .questionText("Q2")
                .language(buildLanguage())
                .category(buildCategory())
                .tags(List.of())
                .build();

        // when
        service.reindexAll(List.of(q1, q2));

        // then
        ArgumentCaptor<List<QuestionDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(elasticsearchOperations).save(captor.capture());

        assertThat(captor.getValue()).hasSize(2)
                .extracting(QuestionDocument::getQuestionText)
                .containsExactlyInAnyOrder("Q1", "Q2");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSaveEmptyList_whenReindexAllCalledWithNoQuestions() {
        // given / when
        service.reindexAll(List.of());

        // then

        ArgumentCaptor<List<QuestionDocument>> captor = ArgumentCaptor.forClass(List.class);
        verify(elasticsearchOperations).save(captor.capture());

        assertThat(captor.getValue()).isEmpty();
    }

    @Test
    void shouldSortTagsAlphabetically_whenMultipleTagsPresent() {
        // given
        Tag tagZ = Tag.builder().id(1L).name("Zebra").build();
        Tag tagA = Tag.builder().id(2L).name("Apple").build();
        Tag tagM = Tag.builder().id(3L).name("Mango").build();
        Question question = Question.builder()
                .id(10L)
                .externalId(UUID.randomUUID())
                .questionText(QUESTION_TEXT)
                .language(buildLanguage())
                .category(buildCategory())
                .tags(List.of(tagZ, tagA, tagM))
                .build();

        // when
        service.index(question);

        // then
        ArgumentCaptor<QuestionDocument> captor = ArgumentCaptor.forClass(QuestionDocument.class);
        verify(elasticsearchOperations).save(captor.capture());

        assertThat(captor.getValue().getTags()).containsExactly("Apple", "Mango", "Zebra");
    }

    private Language buildLanguage() {
        return Language.builder()
                .id(LANGUAGE_ID)
                .name("Java")
                .code(LANGUAGE_CODE)
                .build();
    }

    private Category buildCategory() {
        return Category.builder()
                .id(CATEGORY_ID)
                .name(CATEGORY_NAME)
                .build();
    }
}
