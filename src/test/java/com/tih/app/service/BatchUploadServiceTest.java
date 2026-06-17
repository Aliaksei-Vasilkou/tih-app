package com.tih.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tih.app.dto.BatchAnalyseResponse;
import com.tih.app.dto.BatchCommitRequest;
import com.tih.app.dto.BatchUploadResponse;
import com.tih.app.dto.ConflictResolution;
import com.tih.app.dto.QuestionDto;
import com.tih.app.dto.QuestionTransferItem;
import com.tih.app.mapper.QuestionMapper;
import com.tih.app.model.Category;
import com.tih.app.model.Language;
import com.tih.app.model.Question;
import com.tih.app.model.Tag;
import com.tih.app.repository.CategoryRepository;
import com.tih.app.repository.LanguageRepository;
import com.tih.app.repository.QuestionRepository;
import com.tih.app.repository.TagRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
class BatchUploadServiceTest {

    private static final long LANGUAGE_ID = 1L;
    private static final String LANGUAGE_CODE_GO = "go";
    private static final String LANGUAGE_CODE_JAVA = "java";
    private static final String CORE_CATEGORY = "Core";
    private static final String TAG_NEW = "TagNew";
    private static final String LEVEL_TAG = "L1";
    private static final String QUESTION_TEXT = "What is JVM?";
    private static final String QUESTION_TEXT_ERROR = "Question text is required";
    private static final String UNKNOWN_CATEGORY_ERROR = "UnknownCategory";
    private static final String UNKNOWN_LANG_ERROR = "unknown-lang";

    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private LanguageRepository languageRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private Validator validator;
    @Mock
    private QuestionIndexService questionIndexService;
    @Mock
    private QuestionMapper questionMapper;

    @InjectMocks
    private BatchUploadService service;

    @Test
    @SuppressWarnings("unchecked")
    void shouldReturnFailureResponse_whenFileCannotBeParsed() throws Exception {
        // given
        MultipartFile file = mock(MultipartFile.class);

        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        doThrow(new IOException("bad json"))
                .when(objectMapper).readValue(any(java.io.InputStream.class), any(TypeReference.class));

        // when
        BatchUploadResponse result = service.processUpload(file);

        // then
        assertThat(result.getTotalItems()).isZero();
        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getUpdatedCount()).isZero();
        assertThat(result.getFailureCount()).isZero();
        assertThat(result.getErrors()).hasSize(1)
                .first().asString().contains("Failed to parse file");
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCountAsFailure_whenItemFailsValidation() throws Exception {
        // given
        QuestionTransferItem invalidItem = QuestionTransferItem.builder()
                .question(StringUtils.EMPTY)
                .language(LANGUAGE_CODE_JAVA)
                .category(CORE_CATEGORY)
                .build();
        MultipartFile file = mock(MultipartFile.class);
        ConstraintViolation<QuestionTransferItem> violation = mock(ConstraintViolation.class);

        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        doReturn(List.of(invalidItem))
                .when(objectMapper).readValue(any(java.io.InputStream.class), any(TypeReference.class));
        when(violation.getMessage()).thenReturn(QUESTION_TEXT_ERROR);
        when(validator.validate(invalidItem)).thenReturn(Set.of(violation));

        // when
        BatchUploadResponse result = service.processUpload(file);

        // then
        assertThat(result.getTotalItems()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getUpdatedCount()).isZero();
        assertThat(result.getErrors()).hasSize(1)
                .first().asString().contains(QUESTION_TEXT_ERROR);
        verify(questionRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSkipItem_whenExtIdAlreadyExistsInDatabase() throws Exception {
        // given
        UUID existingId = UUID.randomUUID();
        QuestionTransferItem item = buildItem(existingId, QUESTION_TEXT, LANGUAGE_CODE_JAVA, CORE_CATEGORY);
        MultipartFile file = mock(MultipartFile.class);

        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        doReturn(List.of(item))
                .when(objectMapper).readValue(any(java.io.InputStream.class), any(TypeReference.class));
        when(validator.validate(item)).thenReturn(Set.of());
        when(questionRepository.findByExternalId(existingId))
                .thenReturn(Optional.of(Question.builder().id(LANGUAGE_ID).externalId(existingId).build()));

        // when
        BatchUploadResponse result = service.processUpload(file);

        // then
        assertThat(result.getTotalItems()).isEqualTo(1);
        assertThat(result.getSkippedCount()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isZero();
        assertThat(result.getUpdatedCount()).isZero();
        assertThat(result.getSkipped()).hasSize(1)
                .first().asString().contains(existingId.toString());
        verify(questionRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCountAsFailure_whenLanguageCodeNotFound() throws Exception {
        // given
        QuestionTransferItem item = buildItem(null, QUESTION_TEXT, UNKNOWN_LANG_ERROR, CORE_CATEGORY);
        MultipartFile file = mock(MultipartFile.class);

        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        doReturn(List.of(item))
                .when(objectMapper).readValue(any(java.io.InputStream.class), any(TypeReference.class));
        when(validator.validate(item)).thenReturn(Set.of());
        when(languageRepository.findByCode(UNKNOWN_LANG_ERROR)).thenReturn(Optional.empty());

        // when
        BatchUploadResponse result = service.processUpload(file);

        // then
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1)
                .first().asString().contains("Language not found with code: unknown-lang");
        verify(questionRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldCountAsFailure_whenCategoryNotFoundForLanguage() throws Exception {
        // given
        Language language = buildLanguage();
        QuestionTransferItem item = buildItem(null, QUESTION_TEXT, LANGUAGE_CODE_JAVA, UNKNOWN_CATEGORY_ERROR);
        MultipartFile file = mock(MultipartFile.class);

        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        doReturn(List.of(item))
                .when(objectMapper).readValue(any(java.io.InputStream.class), any(TypeReference.class));
        when(validator.validate(item)).thenReturn(Set.of());
        when(languageRepository.findByCode(LANGUAGE_CODE_JAVA)).thenReturn(Optional.of(language));
        when(categoryRepository.findByNameAndLanguageId(UNKNOWN_CATEGORY_ERROR, LANGUAGE_ID)).thenReturn(Optional.empty());

        // when
        BatchUploadResponse result = service.processUpload(file);

        // then
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1)
                .first().asString().contains(UNKNOWN_CATEGORY_ERROR);
        verify(questionRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSaveQuestion_whenItemIsValidAndAllLookupSucceed() throws Exception {
        // given
        Language language = buildLanguage();
        Category category = buildCategory(language);
        QuestionTransferItem item = buildItem(null, QUESTION_TEXT, LANGUAGE_CODE_JAVA, CORE_CATEGORY);
        Question saved = Question.builder().id(LANGUAGE_ID).questionText(QUESTION_TEXT).build();
        MultipartFile file = mock(MultipartFile.class);

        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        doReturn(List.of(item))
                .when(objectMapper).readValue(any(java.io.InputStream.class), any(TypeReference.class));
        when(validator.validate(item)).thenReturn(Set.of());
        when(languageRepository.findByCode(LANGUAGE_CODE_JAVA)).thenReturn(Optional.of(language));
        when(categoryRepository.findByNameAndLanguageId(CORE_CATEGORY, LANGUAGE_ID)).thenReturn(Optional.of(category));
        when(questionRepository.save(any(Question.class))).thenReturn(saved);

        // when
        BatchUploadResponse result = service.processUpload(file);

        // then
        assertThat(result.getTotalItems()).isEqualTo(1);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isZero();
        assertThat(result.getUpdatedCount()).isZero();
        assertThat(result.getSkippedCount()).isZero();
        verify(questionRepository).save(any(Question.class));
        verify(questionIndexService).index(saved);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldAutoCreateTag_whenTagDoesNotExistForLanguage() throws Exception {
        // given
        Language language = buildLanguage();
        Category category = buildCategory(language);
        QuestionTransferItem item = QuestionTransferItem.builder()
                .question(QUESTION_TEXT)
                .language(LANGUAGE_CODE_JAVA)
                .category(CORE_CATEGORY)
                .tags(List.of(TAG_NEW))
                .build();
        Tag createdTag = Tag.builder().id(99L).name(TAG_NEW).language(language).build();
        Question saved = Question.builder().id(LANGUAGE_ID).questionText(QUESTION_TEXT).build();
        MultipartFile file = mock(MultipartFile.class);

        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        doReturn(List.of(item))
                .when(objectMapper).readValue(any(java.io.InputStream.class), any(TypeReference.class));
        when(validator.validate(item)).thenReturn(Set.of());
        when(languageRepository.findByCode(LANGUAGE_CODE_JAVA)).thenReturn(Optional.of(language));
        when(categoryRepository.findByNameAndLanguageId(CORE_CATEGORY, LANGUAGE_ID)).thenReturn(Optional.of(category));
        when(tagRepository.findByNameIgnoreCaseAndLanguageId(TAG_NEW, LANGUAGE_ID)).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenReturn(createdTag);
        when(questionRepository.save(any(Question.class))).thenReturn(saved);

        // when
        BatchUploadResponse result = service.processUpload(file);

        // then
        assertThat(result.getSuccessCount()).isEqualTo(1);
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldReuseExistingTag_whenTagAlreadyExistsForLanguage() throws Exception {
        // given
        Language language = buildLanguage();
        Category category = buildCategory(language);
        Tag existingTag = Tag.builder().id(5L).name(LEVEL_TAG).language(language).build();
        QuestionTransferItem item = QuestionTransferItem.builder()
                .question(QUESTION_TEXT)
                .language(LANGUAGE_CODE_JAVA)
                .category(CORE_CATEGORY)
                .tags(List.of(LEVEL_TAG))
                .build();
        Question saved = Question.builder().id(LANGUAGE_ID).questionText(QUESTION_TEXT).build();
        MultipartFile file = mock(MultipartFile.class);

        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        doReturn(List.of(item))
                .when(objectMapper).readValue(any(java.io.InputStream.class), any(TypeReference.class));
        when(validator.validate(item)).thenReturn(Set.of());
        when(languageRepository.findByCode(LANGUAGE_CODE_JAVA)).thenReturn(Optional.of(language));
        when(categoryRepository.findByNameAndLanguageId(CORE_CATEGORY, LANGUAGE_ID)).thenReturn(Optional.of(category));
        when(tagRepository.findByNameIgnoreCaseAndLanguageId(LEVEL_TAG, LANGUAGE_ID)).thenReturn(Optional.of(existingTag));
        when(questionRepository.save(any(Question.class))).thenReturn(saved);

        // when
        BatchUploadResponse result = service.processUpload(file);

        // then
        assertThat(result.getSuccessCount()).isEqualTo(1);
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldAggregateCountsCorrectly_whenMixedItemsProvided() throws Exception {
        // given
        UUID existingId = UUID.randomUUID();
        Language language = buildLanguage();
        Category category = buildCategory(language);

        QuestionTransferItem valid = buildItem(null, QUESTION_TEXT, LANGUAGE_CODE_JAVA, CORE_CATEGORY);
        QuestionTransferItem duplicate = buildItem(existingId, "What is GC?", LANGUAGE_CODE_JAVA, CORE_CATEGORY);
        QuestionTransferItem badLang = buildItem(null, "What is X?", "no-such-lang", CORE_CATEGORY);

        Question saved = Question.builder().id(LANGUAGE_ID).questionText(QUESTION_TEXT).build();
        MultipartFile file = mock(MultipartFile.class);

        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        doReturn(List.of(valid, duplicate, badLang))
                .when(objectMapper).readValue(any(java.io.InputStream.class), any(TypeReference.class));
        when(validator.validate(any())).thenReturn(Set.of());
        when(languageRepository.findByCode(LANGUAGE_CODE_JAVA)).thenReturn(Optional.of(language));
        when(categoryRepository.findByNameAndLanguageId(CORE_CATEGORY, LANGUAGE_ID)).thenReturn(Optional.of(category));
        when(questionRepository.findByExternalId(existingId))
                .thenReturn(Optional.of(Question.builder().id(2L).externalId(existingId).build()));
        when(languageRepository.findByCode("no-such-lang")).thenReturn(Optional.empty());
        when(questionRepository.save(any(Question.class))).thenReturn(saved);

        // when
        BatchUploadResponse result = service.processUpload(file);

        // then
        assertThat(result.getTotalItems()).isEqualTo(3);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getSkippedCount()).isEqualTo(1);
        assertThat(result.getFailureCount()).isEqualTo(1);
        assertThat(result.getUpdatedCount()).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void analyseUpload_shouldSplitNewItemsAndDuplicates_whenExtIdExists() throws Exception {
        // given
        UUID duplicateExtId = UUID.randomUUID();
        QuestionTransferItem newItem = buildItem(null, QUESTION_TEXT, LANGUAGE_CODE_JAVA, CORE_CATEGORY);
        QuestionTransferItem duplicateItem = buildItem(duplicateExtId, "What is inheritance?", LANGUAGE_CODE_JAVA, CORE_CATEGORY);
        MultipartFile file = mock(MultipartFile.class);
        Language language = buildLanguage();
        Category category = buildCategory(language);
        Question existingQuestion = Question.builder()
                .id(21L)
                .externalId(duplicateExtId)
                .questionText("Existing")
                .build();

        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        doReturn(List.of(newItem, duplicateItem))
                .when(objectMapper).readValue(any(java.io.InputStream.class), any(TypeReference.class));
        when(validator.validate(any())).thenReturn(Set.of());
        when(languageRepository.findByCode(LANGUAGE_CODE_JAVA)).thenReturn(Optional.of(language));
        when(categoryRepository.findByNameAndLanguageId(CORE_CATEGORY, LANGUAGE_ID)).thenReturn(Optional.of(category));
        when(questionRepository.findByExternalId(duplicateExtId)).thenReturn(Optional.of(existingQuestion));
        when(questionMapper.toDto(existingQuestion)).thenReturn(QuestionDto.builder().id(21L).questionText("Existing").build());

        // when
        BatchAnalyseResponse response = service.analyseUpload(file);

        // then
        assertThat(response.newItems()).hasSize(1);
        assertThat(response.duplicates()).hasSize(1);
        assertThat(response.duplicates().getFirst().extId()).isEqualTo(duplicateExtId);
        verify(questionRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void analyseUpload_shouldThrowIllegalArgument_whenValidationFails() throws Exception {
        // given
        QuestionTransferItem invalidItem = QuestionTransferItem.builder()
                .question(StringUtils.EMPTY)
                .language(LANGUAGE_CODE_JAVA)
                .category(CORE_CATEGORY)
                .build();
        MultipartFile file = mock(MultipartFile.class);
        ConstraintViolation<QuestionTransferItem> violation = mock(ConstraintViolation.class);

        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        doReturn(List.of(invalidItem))
                .when(objectMapper).readValue(any(java.io.InputStream.class), any(TypeReference.class));
        when(violation.getMessage()).thenReturn(QUESTION_TEXT_ERROR);
        when(validator.validate(invalidItem)).thenReturn(Set.of(violation));

        // when
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() -> service.analyseUpload(file));

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(QUESTION_TEXT_ERROR);
    }

    @Test
    void commitUpload_shouldInsertNewAndUpdateAcceptedAndSkipSkipped() {
        // given
        UUID acceptedExtId = UUID.randomUUID();
        UUID skippedExtId = UUID.randomUUID();
        Language language = buildLanguage();
        Category category = buildCategory(language);
        QuestionTransferItem newItem = buildItem(null, QUESTION_TEXT, LANGUAGE_CODE_JAVA, CORE_CATEGORY);
        QuestionTransferItem incomingDuplicate = buildItem(acceptedExtId, "Updated question", LANGUAGE_CODE_JAVA, CORE_CATEGORY);
        Question existing = Question.builder()
                .id(7L)
                .externalId(acceptedExtId)
                .questionText("Old question")
                .language(language)
                .category(category)
                .build();
        BatchCommitRequest request = new BatchCommitRequest(
                List.of(newItem, incomingDuplicate),
                List.of(
                        new ConflictResolution(acceptedExtId, "accept"),
                        new ConflictResolution(skippedExtId, "skip")));

        when(validator.validate(any())).thenReturn(Set.of());
        when(languageRepository.findByCode(LANGUAGE_CODE_JAVA)).thenReturn(Optional.of(language));
        when(categoryRepository.findByNameAndLanguageId(CORE_CATEGORY, LANGUAGE_ID)).thenReturn(Optional.of(category));
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(questionRepository.findByExternalId(acceptedExtId)).thenReturn(Optional.of(existing));

        // when
        BatchUploadResponse response = service.commitUpload(request);

        // then
        assertThat(response.getTotalItems()).isEqualTo(3);
        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getUpdatedCount()).isEqualTo(1);
        assertThat(response.getSkippedCount()).isEqualTo(1);
        assertThat(response.getFailureCount()).isZero();
        assertThat(response.getSkipped()).containsExactly(skippedExtId.toString());
    }

    @Test
    void commitUpload_shouldThrowIllegalArgument_whenRequestHasNoWork() {
        // given
        BatchCommitRequest request = new BatchCommitRequest(List.of(), List.of());

        // when
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() -> service.commitUpload(request));

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("At least one of newItems or resolutions must be provided");
    }

    @Test
    void commitUpload_shouldThrowIllegalArgument_whenRequestIsNull() {
        // given
        BatchCommitRequest request = null;

        // when
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() -> service.commitUpload(request));

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Request body is required");
    }

    @Test
    void commitUpload_shouldThrowIllegalArgument_whenResolutionActionIsInvalid() {
        // given
        UUID duplicateExtId = UUID.randomUUID();
        BatchCommitRequest request = new BatchCommitRequest(
                List.of(),
                List.of(new ConflictResolution(duplicateExtId, "overwrite")));

        // when
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() -> service.commitUpload(request));

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("action must be 'accept' or 'skip'");
    }

    @Test
    void commitUpload_shouldThrowIllegalArgument_whenAcceptedResolutionHasNoIncomingPayload() {
        // given
        UUID duplicateExtId = UUID.randomUUID();
        BatchCommitRequest request = new BatchCommitRequest(
                List.of(),
                List.of(new ConflictResolution(duplicateExtId, "accept")));

        // when
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() -> service.commitUpload(request));

        // then
        assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires incoming item in newItems");
    }

    @Test
    void commitUpload_shouldCountFailure_whenAcceptedResolutionHasNoExistingQuestion() {
        // given
        UUID duplicateExtId = UUID.randomUUID();
        QuestionTransferItem incomingDuplicate = buildItem(duplicateExtId, QUESTION_TEXT, LANGUAGE_CODE_JAVA, CORE_CATEGORY);
        BatchCommitRequest request = new BatchCommitRequest(
                List.of(incomingDuplicate),
                List.of(new ConflictResolution(duplicateExtId, "accept")));

        when(questionRepository.findByExternalId(duplicateExtId)).thenReturn(Optional.empty());

        // when
        BatchUploadResponse response = service.commitUpload(request);

        // then
        assertThat(response.getTotalItems()).isEqualTo(1);
        assertThat(response.getFailureCount()).isEqualTo(1);
        assertThat(response.getUpdatedCount()).isZero();
        assertThat(response.getErrors()).hasSize(1)
                .first().asString().contains("Existing question not found");
    }

    @Test
    @SuppressWarnings("unchecked")
    void analyseUpload_shouldThrowUnknownLanguageException_whenLanguageIsUnknown() throws Exception {
        // given
        QuestionTransferItem item = buildItem(null, QUESTION_TEXT, UNKNOWN_LANG_ERROR, CORE_CATEGORY);
        MultipartFile file = mock(MultipartFile.class);

        when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        doReturn(List.of(item))
                .when(objectMapper).readValue(any(java.io.InputStream.class), any(TypeReference.class));
        when(validator.validate(item)).thenReturn(Set.of());
        when(languageRepository.findByCode(UNKNOWN_LANG_ERROR)).thenReturn(Optional.empty());

        // when
        Throwable thrown = org.assertj.core.api.Assertions.catchThrowable(() -> service.analyseUpload(file));

        // then
        assertThat(thrown)
                .isInstanceOf(com.tih.app.exception.UnknownLanguageException.class)
                .hasMessageContaining(UNKNOWN_LANG_ERROR);
    }

    @Test
    void shouldReturnMappedTransferItems_whenQuestionsExist() {
        // given
        Language language = buildLanguage();
        Category category = buildCategory(language);
        UUID extId = UUID.randomUUID();
        Question q = Question.builder()
                .id(LANGUAGE_ID)
                .externalId(extId)
                .questionText(QUESTION_TEXT)
                .answerContent("JVM is...")
                .language(language)
                .category(category)
                .tags(List.of())
                .build();

        when(questionRepository.findAllForExport(null, null)).thenReturn(List.of(q));

        // when
        List<QuestionTransferItem> result = service.exportQuestions(null, null);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getExtId()).isEqualTo(extId);
        assertThat(result.getFirst().getQuestion()).isEqualTo(QUESTION_TEXT);
        assertThat(result.getFirst().getLanguage()).isEqualTo(LANGUAGE_CODE_JAVA);
        assertThat(result.getFirst().getCategory()).isEqualTo(CORE_CATEGORY);
    }

    @Test
    void shouldReturnEmptyList_whenNoQuestionsMatchExportFilter() {
        // given
        when(questionRepository.findAllForExport(LANGUAGE_CODE_GO, CORE_CATEGORY)).thenReturn(List.of());

        // when
        List<QuestionTransferItem> result = service.exportQuestions(LANGUAGE_CODE_GO, CORE_CATEGORY);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldPassNullFilters_whenExportCalledWithBlankStrings() {
        // given
        when(questionRepository.findAllForExport(null, null)).thenReturn(List.of());

        // when
        service.exportQuestions(StringUtils.SPACE, StringUtils.SPACE);

        // then
        verify(questionRepository).findAllForExport(null, null);
    }

    private QuestionTransferItem buildItem(UUID extId, String question, String language, String category) {
        return QuestionTransferItem.builder()
                .extId(extId)
                .question(question)
                .language(language)
                .category(category)
                .build();
    }

    private Language buildLanguage() {
        return Language.builder()
                .id(LANGUAGE_ID)
                .name("Java")
                .code(LANGUAGE_CODE_JAVA)
                .build();
    }

    private Category buildCategory(Language language) {
        return Category.builder()
                .id(10L)
                .name(CORE_CATEGORY)
                .language(language)
                .build();
    }
}
