package com.tih.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.tih.app.dto.PageResponse;
import com.tih.app.dto.QuestionCreateRequest;
import com.tih.app.dto.QuestionDto;
import com.tih.app.dto.QuestionSearchRequest;
import com.tih.app.exception.ResourceNotFoundException;
import com.tih.app.mapper.QuestionMapper;
import com.tih.app.model.Category;
import com.tih.app.model.Language;
import com.tih.app.model.Question;
import com.tih.app.model.Tag;
import com.tih.app.repository.CategoryRepository;
import com.tih.app.repository.LanguageRepository;
import com.tih.app.repository.QuestionRepository;
import com.tih.app.repository.TagRepository;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    private static final long LANGUAGE_ID = 2L;
    private static final long GENERAL_LANGUAGE_ID = 1L;
    private static final long CATEGORY_ID = 10L;
    private static final long NON_EXISTENT_ID = 99L;
    private static final long QUESTION_ID = 1L;
    private static final String QUESTION_TEXT = "What is JVM?";
    private static final String SEARCH_QUERY_GC = "JVM garbage collection";
    private static final String SEARCH_QUERY_JVM = "JVM";

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private LanguageRepository languageRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private QuestionMapper questionMapper;
    @Mock
    private QuestionIndexService questionIndexService;
    @Mock
    private QuestionSearchService questionSearchService;

    @InjectMocks
    private QuestionService service;

    @Test
    void shouldReturnAllQuestions_whenNoFiltersProvided() {
        // given
        Page<Question> page = new PageImpl<>(List.of(buildQuestion()));

        when(questionRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(questionMapper.toDtoList(any())).thenReturn(List.of(buildQuestionDto()));

        // when
        PageResponse<QuestionDto> result = service.findAll(null, null, 0, 10);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(questionRepository).findAll(any(Pageable.class));
    }

    @Test
    void shouldFilterByLanguageAndCategory_whenBothFiltersProvided() {
        // given — languageId=2 resolves to [2, 1] (GENERAL always included)
        List<Long> expectedIds = List.of(LANGUAGE_ID, GENERAL_LANGUAGE_ID);
        Page<Question> page = new PageImpl<>(List.of(buildQuestion()));

        when(questionRepository.findAllByLanguageIdInAndCategoryId(eq(expectedIds), eq(CATEGORY_ID), any(Pageable.class))).thenReturn(page);
        when(questionMapper.toDtoList(any())).thenReturn(List.of(buildQuestionDto()));

        // when
        PageResponse<QuestionDto> result = service.findAll(LANGUAGE_ID, CATEGORY_ID, 0, 10);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(questionRepository).findAllByLanguageIdInAndCategoryId(eq(expectedIds), eq(CATEGORY_ID), any(Pageable.class));
    }

    @Test
    void shouldFilterByLanguageOnly_whenOnlyLanguageFilterProvided() {
        // given
        List<Long> expectedIds = List.of(LANGUAGE_ID, GENERAL_LANGUAGE_ID);
        Page<Question> page = new PageImpl<>(List.of(buildQuestion()));

        when(questionRepository.findAllByLanguageIdIn(eq(expectedIds), any(Pageable.class))).thenReturn(page);
        when(questionMapper.toDtoList(any())).thenReturn(List.of(buildQuestionDto()));

        // when
        PageResponse<QuestionDto> result = service.findAll(LANGUAGE_ID, null, 0, 10);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(questionRepository).findAllByLanguageIdIn(eq(expectedIds), any(Pageable.class));
    }

    @Test
    void shouldFilterByCategoryOnly_whenOnlyCategoryFilterProvided() {
        // given
        Page<Question> page = new PageImpl<>(List.of(buildQuestion()));

        when(questionRepository.findAllByCategoryId(eq(CATEGORY_ID), any(Pageable.class))).thenReturn(page);
        when(questionMapper.toDtoList(any())).thenReturn(List.of(buildQuestionDto()));

        // when
        PageResponse<QuestionDto> result = service.findAll(null, CATEGORY_ID, 0, 10);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(questionRepository).findAllByCategoryId(eq(CATEGORY_ID), any(Pageable.class));
    }

    @Test
    void shouldIncludeOnlyGeneralLanguage_whenFilteredByGeneralLanguageId() {
        // given
        List<Long> expectedIds = List.of(GENERAL_LANGUAGE_ID);
        Page<Question> page = new PageImpl<>(List.of());

        when(questionRepository.findAllByLanguageIdIn(eq(expectedIds), any(Pageable.class))).thenReturn(page);
        when(questionMapper.toDtoList(any())).thenReturn(List.of());

        // when
        PageResponse<QuestionDto> result = service.findAll(GENERAL_LANGUAGE_ID, null, 0, 10);

        // then
        assertThat(result.getContent()).isEmpty();
        verify(questionRepository).findAllByLanguageIdIn(eq(expectedIds), any(Pageable.class));
    }

    @Test
    void shouldReturnQuestionDto_whenQuestionExists() {
        // given
        Question question = buildQuestion();
        QuestionDto dto = buildQuestionDto();

        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(questionMapper.toDto(question)).thenReturn(dto);

        // when
        QuestionDto result = service.findById(QUESTION_ID);

        // then
        assertThat(result.getId()).isEqualTo(QUESTION_ID);
    }

    @Test
    void shouldThrowResourceNotFoundException_whenQuestionNotFound() {
        // given
        when(questionRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.findById(NON_EXISTENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTENT_ID));
    }

    @Test
    void shouldDelegateToFindAll_whenSearchQueryIsBlank() {
        // given
        QuestionSearchRequest request = new QuestionSearchRequest("  ", null, null, 0, 10);
        Page<Question> page = new PageImpl<>(List.of());

        when(questionRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(questionMapper.toDtoList(any())).thenReturn(List.of());

        // when
        PageResponse<QuestionDto> result = service.search(request);

        // then
        verify(questionSearchService, never()).search(any(), any(), any(), any());
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void shouldDelegateToFindAll_whenSearchQueryIsNull() {
        // given
        QuestionSearchRequest request = new QuestionSearchRequest(null, null, null, 0, 10);
        Page<Question> page = new PageImpl<>(List.of());

        when(questionRepository.findAll(any(Pageable.class))).thenReturn(page);
        when(questionMapper.toDtoList(any())).thenReturn(List.of());

        // when
        PageResponse<QuestionDto> result = service.search(request);

        // then
        verify(questionSearchService, never()).search(any(), any(), any(), any());
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void shouldDelegateToElasticsearch_whenQueryIsPresent() {
        // given
        QuestionSearchRequest request = new QuestionSearchRequest(SEARCH_QUERY_GC, null, null, 0, 10);
        PageResponse<QuestionDto> esResponse = PageResponse.<QuestionDto>builder()
                .content(List.of(buildQuestionDto()))
                .totalElements(1)
                .build();

        when(questionSearchService.search(eq(SEARCH_QUERY_GC), any(), any(), any()))
                .thenReturn(esResponse);

        // when
        PageResponse<QuestionDto> result = service.search(request);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(questionSearchService).search(eq(SEARCH_QUERY_GC), any(), any(), any());
        verify(questionRepository, never()).searchByFullText(any(), any(), any(), any());
    }

    @Test
    void shouldFallBackToPostgresFTS_whenElasticsearchThrowsException() {
        // given — query >= 3 chars triggers the FTS path
        QuestionSearchRequest request = new QuestionSearchRequest(SEARCH_QUERY_JVM, null, null, 0, 10);
        Page<Question> page = new PageImpl<>(List.of(buildQuestion()));

        when(questionSearchService.search(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("ES unavailable"));
        when(questionRepository.searchByFullText(eq(SEARCH_QUERY_JVM), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);
        when(questionMapper.toDtoList(any())).thenReturn(List.of(buildQuestionDto()));

        // when
        PageResponse<QuestionDto> result = service.search(request);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(questionRepository).searchByFullText(eq(SEARCH_QUERY_JVM), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void shouldUseFallbackKeywordSearch_whenQueryIsShorterThanMinFtsLength() {
        // given — query < 3 chars falls back to keyword search
        QuestionSearchRequest request = new QuestionSearchRequest("GC", null, null, 0, 10);
        Page<Question> page = new PageImpl<>(List.of(buildQuestion()));

        when(questionSearchService.search(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("ES unavailable"));
        when(questionRepository.searchByKeyword(eq("GC"), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);
        when(questionMapper.toDtoList(any())).thenReturn(List.of(buildQuestionDto()));

        // when
        PageResponse<QuestionDto> result = service.search(request);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(questionRepository).searchByKeyword(eq("GC"), isNull(), isNull(), any(Pageable.class));
        verify(questionRepository, never()).searchByFullText(any(), any(), any(), any());
    }

    @Test
    void shouldUseFallbackWithLanguageIds_whenLanguageFilterSetAndEsFails() {
        // given
        QuestionSearchRequest request = new QuestionSearchRequest(SEARCH_QUERY_JVM, LANGUAGE_ID, null, 0, 10);
        List<Long> expectedIds = List.of(LANGUAGE_ID, GENERAL_LANGUAGE_ID);
        Page<Question> page = new PageImpl<>(List.of(buildQuestion()));

        when(questionSearchService.search(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("ES unavailable"));
        when(questionRepository.searchByFullTextWithLanguageIds(eq(SEARCH_QUERY_JVM), eq(expectedIds), isNull(), any(Pageable.class)))
                .thenReturn(page);
        when(questionMapper.toDtoList(any())).thenReturn(List.of(buildQuestionDto()));

        // when
        PageResponse<QuestionDto> result = service.search(request);

        // then
        assertThat(result.getContent()).hasSize(1);
        verify(questionRepository).searchByFullTextWithLanguageIds(eq(SEARCH_QUERY_JVM), eq(expectedIds), isNull(), any(Pageable.class));
    }

    @Test
    void shouldCreateQuestion_whenLanguageAndCategoryExist() {
        // given
        Language language = buildLanguage();
        Category category = buildCategory();
        QuestionCreateRequest request = buildCreateRequest(List.of());
        Question entity = buildQuestion();
        QuestionDto dto = buildQuestionDto();

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(questionMapper.toEntity(request)).thenReturn(entity);
        when(questionRepository.save(entity)).thenReturn(entity);
        when(questionMapper.toDto(entity)).thenReturn(dto);

        // when
        QuestionDto result = service.create(request);

        // then
        assertThat(result.getId()).isEqualTo(QUESTION_ID);
        verify(questionRepository).save(entity);
        verify(questionIndexService).index(entity);
    }

    @Test
    void shouldThrowResourceNotFoundException_whenLanguageNotFoundOnCreate() {
        // given
        QuestionCreateRequest request = buildCreateRequest(List.of());

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(LANGUAGE_ID));
        verify(questionRepository, never()).save(any());
    }

    @Test
    void shouldThrowResourceNotFoundException_whenCategoryNotFoundOnCreate() {
        // given
        Language language = buildLanguage();
        QuestionCreateRequest request = buildCreateRequest(List.of());

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(CATEGORY_ID));
        verify(questionRepository, never()).save(any());
    }

    @Test
    void shouldCreateQuestionWithTags_whenTagIdsProvided() {
        // given
        Language language = buildLanguage();
        Category category = buildCategory();
        Tag tag = Tag.builder().id(5L).name("L1").language(language).build();
        QuestionCreateRequest request = buildCreateRequest(List.of(5L));
        Question entity = buildQuestion();
        QuestionDto dto = buildQuestionDto();

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(tagRepository.findAllById(List.of(5L))).thenReturn(List.of(tag));
        when(questionMapper.toEntity(request)).thenReturn(entity);
        when(questionRepository.save(entity)).thenReturn(entity);
        when(questionMapper.toDto(entity)).thenReturn(dto);

        // when
        QuestionDto result = service.create(request);

        // then
        assertThat(result).isNotNull();
        verify(tagRepository).findAllById(List.of(5L));
    }

    @Test
    void shouldCreateQuestionWithNoTags_whenTagIdsIsEmpty() {
        // given
        Language language = buildLanguage();
        Category category = buildCategory();
        QuestionCreateRequest request = buildCreateRequest(List.of());
        Question entity = buildQuestion();
        QuestionDto dto = buildQuestionDto();

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(questionMapper.toEntity(request)).thenReturn(entity);
        when(questionRepository.save(entity)).thenReturn(entity);
        when(questionMapper.toDto(entity)).thenReturn(dto);

        // when
        service.create(request);

        // then
        verify(tagRepository, never()).findAllById(any());
    }

    @Test
    void shouldUpdateQuestion_whenQuestionLanguageAndCategoryExist() {
        // given
        Language language = buildLanguage();
        Category category = buildCategory();
        Question question = buildQuestion();
        question.setTags(new ArrayList<>());
        QuestionCreateRequest request = buildCreateRequest(List.of());
        QuestionDto dto = buildQuestionDto();

        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(questionRepository.save(question)).thenReturn(question);
        when(questionMapper.toDto(question)).thenReturn(dto);

        // when
        QuestionDto result = service.update(QUESTION_ID, request);

        // then
        assertThat(result).isNotNull();
        verify(questionMapper).updateEntity(request, question);
        verify(questionRepository).save(question);
        verify(questionIndexService).index(question);
    }

    @Test
    void shouldThrowResourceNotFoundException_whenUpdatingNonExistentQuestion() {
        // given
        when(questionRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.update(NON_EXISTENT_ID, buildCreateRequest(List.of())))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTENT_ID));
        verify(questionRepository, never()).save(any());
    }

    @Test
    void shouldThrowResourceNotFoundException_whenLanguageNotFoundOnUpdate() {
        // given
        Question question = buildQuestion();

        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.update(QUESTION_ID, buildCreateRequest(List.of())))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(LANGUAGE_ID));
        verify(questionRepository, never()).save(any());
    }

    @Test
    void shouldThrowResourceNotFoundException_whenCategoryNotFoundOnUpdate() {
        // given
        Language language = buildLanguage();
        Question question = buildQuestion();

        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));
        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.update(QUESTION_ID, buildCreateRequest(List.of())))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(CATEGORY_ID));
        verify(questionRepository, never()).save(any());
    }

    @Test
    void shouldDeleteQuestion_whenQuestionExists() {
        // given
        Question question = buildQuestion();

        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question));

        // when
        service.delete(QUESTION_ID);

        // then
        verify(questionRepository).deleteById(QUESTION_ID);
        verify(questionIndexService).delete(QUESTION_ID);
    }

    @Test
    void shouldThrowResourceNotFoundException_whenDeletingNonExistentQuestion() {
        // given
        when(questionRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.delete(NON_EXISTENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTENT_ID));
        verify(questionRepository, never()).deleteById(any());
        verify(questionIndexService, never()).delete(any());
    }

    private Question buildQuestion() {
        return Question.builder()
                .id(QuestionServiceTest.QUESTION_ID)
                .externalId(UUID.randomUUID())
                .questionText(QUESTION_TEXT)
                .answerContent("JVM is...")
                .language(buildLanguage())
                .category(buildCategory())
                .build();
    }

    private QuestionDto buildQuestionDto() {
        return QuestionDto.builder()
                .id(QuestionServiceTest.QUESTION_ID)
                .questionText(QUESTION_TEXT)
                .build();
    }

    private Language buildLanguage() {
        return Language.builder()
                .id(LANGUAGE_ID)
                .name("Java")
                .code("java")
                .build();
    }

    private Category buildCategory() {
        return Category.builder()
                .id(CATEGORY_ID)
                .name("Core")
                .build();
    }

    private QuestionCreateRequest buildCreateRequest(List<Long> tagIds) {
        return QuestionCreateRequest.builder()
                .questionText(QUESTION_TEXT)
                .languageId(LANGUAGE_ID)
                .categoryId(CATEGORY_ID)
                .tagIds(tagIds)
                .build();
    }
}
