package com.tih.app.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tih.app.dto.BatchAnalyseResponse;
import com.tih.app.dto.BatchCommitRequest;
import com.tih.app.dto.BatchUploadResponse;
import com.tih.app.dto.ConflictResolution;
import com.tih.app.dto.DuplicateConflict;
import com.tih.app.dto.QuestionTransferItem;
import com.tih.app.exception.UnknownLanguageException;
import com.tih.app.mapper.QuestionMapper;
import com.tih.app.model.Category;
import com.tih.app.model.Language;
import com.tih.app.model.Question;
import com.tih.app.model.Tag;
import com.tih.app.repository.CategoryRepository;
import com.tih.app.repository.LanguageRepository;
import com.tih.app.repository.QuestionRepository;
import com.tih.app.repository.TagRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchUploadService {

    private static final String ITEM_PREFIX = "Item ";
    private static final String ACCEPT_ACTION = "accept";
    private static final String SKIP_ACTION = "skip";

    private final QuestionRepository questionRepository;
    private final LanguageRepository languageRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final QuestionIndexService questionIndexService;
    private final QuestionMapper questionMapper;

    @Transactional
    public BatchUploadResponse processUpload(MultipartFile file) {
        BatchUploadResponse response;
        List<QuestionTransferItem> items = new ArrayList<>();

        try {
            items = parseTransferItems(file);
        }
        catch (Exception e) {
            log.error("Failed to parse upload file", e);
            response = BatchUploadResponse.builder()
                    .totalItems(0)
                    .successCount(0)
                    .updatedCount(0)
                    .failureCount(0)
                    .skippedCount(0)
                    .errors(List.of("Failed to parse file: " + e.getMessage()))
                    .skipped(List.of())
                    .build();

            return response;
        }

        int success = 0, failure = 0, skipped = 0;
        List<String> errors = new ArrayList<>();
        List<String> skippedMessages = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            QuestionTransferItem item = items.get(i);
            try {
                Set<ConstraintViolation<QuestionTransferItem>> violations = validator.validate(item);
                if (!violations.isEmpty()) {
                    String msg = ITEM_PREFIX + (i + 1) + ": " + formatViolations(violations);
                    errors.add(msg);
                    failure++;
                    continue;
                }

                ItemStatus status = processItem(item, i + 1, skippedMessages);
                switch (status) {
                    case SAVED -> success++;
                    case SKIPPED -> skipped++;
                    case FAILED -> failure++;
                }
            }
            catch (Exception e) {
                log.warn("Failed to process item {}: {}", i + 1, e.getMessage());
                errors.add(ITEM_PREFIX + (i + 1) + ": " + e.getMessage());
                failure++;
            }
        }

        log.info("Batch upload completed. Saved: {}, Skipped: {}, Failed: {}", success, skipped, failure);
        response = BatchUploadResponse.builder()
                .totalItems(items.size())
                .successCount(success)
                .updatedCount(0)
                .failureCount(failure)
                .skippedCount(skipped)
                .errors(errors)
                .skipped(skippedMessages)
                .build();

        return response;
    }

    @Transactional(readOnly = true)
    public BatchAnalyseResponse analyseUpload(MultipartFile file) {
        List<QuestionTransferItem> items;

        try {
            items = parseTransferItems(file);
        }
        catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse file: " + e.getMessage());
        }

        List<QuestionTransferItem> newItems = new ArrayList<>();
        List<DuplicateConflict> duplicates = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            QuestionTransferItem item = items.get(i);
            validateTransferItemOrThrow(item, i + 1);
            validateCategoryAndLanguage(item);

            if (item.getExtId() == null) {
                newItems.add(item);
                continue;
            }

            Optional<Question> existing = questionRepository.findByExternalId(item.getExtId());
            if (existing.isPresent()) {
                duplicates.add(new DuplicateConflict(item.getExtId(), questionMapper.toDto(existing.get()), item));
                continue;
            }

            newItems.add(item);
        }

        return new BatchAnalyseResponse(newItems, duplicates);
    }

    @Transactional
    public BatchUploadResponse commitUpload(BatchCommitRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request body is required");
        }

        List<QuestionTransferItem> newItems = request.newItems() == null ? List.of() : request.newItems();
        List<ConflictResolution> resolutions = request.resolutions() == null ? List.of() : request.resolutions();

        if (newItems.isEmpty() && resolutions.isEmpty()) {
            throw new IllegalArgumentException("At least one of newItems or resolutions must be provided");
        }

        Map<UUID, QuestionTransferItem> incomingByExtId = indexIncomingItemsByExtId(newItems);
        Set<UUID> acceptedExtIds = validateAcceptResolutions(resolutions, incomingByExtId);

        int successCount = 0;
        int updatedCount = 0;
        int failureCount = 0;
        int skippedCount = 0;
        List<String> errors = new ArrayList<>();
        List<String> skipped = new ArrayList<>();

        for (int i = 0; i < newItems.size(); i++) {
            QuestionTransferItem item = newItems.get(i);

            if (item.getExtId() != null && acceptedExtIds.contains(item.getExtId())) {
                continue;
            }

            try {
                validateTransferItemOrThrow(item, i + 1);
                createQuestion(item);
                successCount++;
            }
            catch (Exception e) {
                failureCount++;
                errors.add(ITEM_PREFIX + (i + 1) + ": " + e.getMessage());
            }
        }

        for (int i = 0; i < resolutions.size(); i++) {
            ConflictResolution resolution = resolutions.get(i);
            try {
                validateResolutionOrThrow(resolution, i + 1);

                if (SKIP_ACTION.equalsIgnoreCase(resolution.action())) {
                    skippedCount++;
                    skipped.add(resolution.extId().toString());
                    continue;
                }

                QuestionTransferItem incomingItem = incomingByExtId.get(resolution.extId());
                if (incomingItem == null) {
                    throw new IllegalArgumentException("Incoming duplicate payload not found for extId: " + resolution.extId());
                }

                Question existing = questionRepository.findByExternalId(resolution.extId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Existing question not found for extId: " + resolution.extId()));

                validateTransferItemOrThrow(incomingItem, i + 1);
                updateExistingQuestion(existing, incomingItem);
                updatedCount++;
            }
            catch (Exception e) {
                failureCount++;
                errors.add("Resolution " + (i + 1) + ": " + e.getMessage());
            }
        }

        int totalItems = successCount + updatedCount + failureCount + skippedCount;

        return BatchUploadResponse.builder()
                .totalItems(totalItems)
                .successCount(successCount)
                .updatedCount(updatedCount)
                .failureCount(failureCount)
                .skippedCount(skippedCount)
                .errors(errors)
                .skipped(skipped)
                .build();
    }

    @Transactional(readOnly = true)
    public List<QuestionTransferItem> exportQuestions(String languageCode, String categoryName) {
        List<Question> questions = questionRepository.findAllForExport(
                languageCode != null && !languageCode.isBlank() ? languageCode : null,
                categoryName != null && !categoryName.isBlank() ? categoryName : null);

        log.info("Exporting {} question(s) [languageCode={}, categoryName={}]",
                questions.size(), languageCode, categoryName);

        return questions.stream()
                .map(q -> QuestionTransferItem.builder()
                        .extId(q.getExternalId())
                        .question(q.getQuestionText())
                        .answer(q.getAnswerContent())
                        .language(q.getLanguage().getCode())
                        .category(q.getCategory().getName())
                        .tags(q.getTags().stream().map(Tag::getName).sorted().toList())
                        .build())
                .toList();
    }

    private List<QuestionTransferItem> parseTransferItems(MultipartFile file) throws Exception {
        return objectMapper.readValue(file.getInputStream(), new TypeReference<>() {
        });
    }

    private void validateTransferItemOrThrow(QuestionTransferItem item, int index) {
        Set<ConstraintViolation<QuestionTransferItem>> violations = validator.validate(item);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException(ITEM_PREFIX + index + ": " + formatViolations(violations));
        }
    }

    private String formatViolations(Set<ConstraintViolation<QuestionTransferItem>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage)
                .reduce((a, b) -> a + "; " + b).orElse("");
    }

    private void validateResolutionOrThrow(ConflictResolution resolution, int index) {
        if (resolution == null) {
            throw new IllegalArgumentException("Resolution " + index + ": payload is required");
        }
        if (resolution.extId() == null) {
            throw new IllegalArgumentException("Resolution " + index + ": extId is required");
        }
        if (resolution.action() == null || resolution.action().isBlank()) {
            throw new IllegalArgumentException("Resolution " + index + ": action is required");
        }
        if (!ACCEPT_ACTION.equalsIgnoreCase(resolution.action()) && !SKIP_ACTION.equalsIgnoreCase(resolution.action())) {
            throw new IllegalArgumentException("Resolution " + index + ": action must be 'accept' or 'skip'");
        }
    }

    private Map<UUID, QuestionTransferItem> indexIncomingItemsByExtId(List<QuestionTransferItem> newItems) {
        Map<UUID, QuestionTransferItem> incomingByExtId = new HashMap<>();

        for (QuestionTransferItem item : newItems) {
            if (item.getExtId() != null) {
                incomingByExtId.put(item.getExtId(), item);
            }
        }

        return incomingByExtId;
    }

    private Set<UUID> validateAcceptResolutions(List<ConflictResolution> resolutions,
            Map<UUID, QuestionTransferItem> incomingByExtId) {
        Set<UUID> acceptedExtIds = new HashSet<>();
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < resolutions.size(); i++) {
            ConflictResolution resolution = resolutions.get(i);

            try {
                validateResolutionOrThrow(resolution, i + 1);
            }
            catch (IllegalArgumentException ex) {
                errors.add(ex.getMessage());
                continue;
            }

            if (!ACCEPT_ACTION.equalsIgnoreCase(resolution.action())) {
                continue;
            }

            acceptedExtIds.add(resolution.extId());

            if (!incomingByExtId.containsKey(resolution.extId())) {
                errors.add("Resolution extId=" + resolution.extId()
                        + " with action=accept requires incoming item in newItems");
            }
        }

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errors));
        }

        return acceptedExtIds;
    }

    private void validateCategoryAndLanguage(QuestionTransferItem item) {
        Language language = resolveLanguage(item.getLanguage());
        resolveCategory(item.getCategory(), language.getId(), item.getLanguage());
    }

    private void createQuestion(QuestionTransferItem item) {
        Language language = resolveLanguage(item.getLanguage());
        Category category = resolveCategory(item.getCategory(), language.getId(), item.getLanguage());
        List<Tag> tags = resolveOrCreateTags(item.getTags(), language);

        Question question = Question.builder()
                .externalId(item.getExtId())
                .questionText(item.getQuestion())
                .answerContent(item.getAnswer())
                .language(language)
                .category(category)
                .tags(tags)
                .build();

        Question saved = questionRepository.save(question);
        questionIndexService.index(saved);
    }

    private void updateExistingQuestion(Question existing, QuestionTransferItem incomingItem) {
        Language language = resolveLanguage(incomingItem.getLanguage());
        Category category = resolveCategory(incomingItem.getCategory(), language.getId(), incomingItem.getLanguage());
        List<Tag> tags = resolveOrCreateTags(incomingItem.getTags(), language);

        existing.setQuestionText(incomingItem.getQuestion());
        existing.setAnswerContent(incomingItem.getAnswer());
        existing.setLanguage(language);
        existing.setCategory(category);
        existing.setTags(tags);

        Question saved = questionRepository.save(existing);
        questionIndexService.index(saved);
    }

    private Language resolveLanguage(String languageCode) {
        return languageRepository.findByCode(languageCode)
                .orElseThrow(() -> new UnknownLanguageException(languageCode));
    }

    private Category resolveCategory(String categoryName, Long languageId, String languageCode) {
        return categoryRepository.findByNameAndLanguageId(categoryName, languageId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Category '" + categoryName + "' not found for language: " + languageCode));
    }

    private ItemStatus processItem(QuestionTransferItem item, int index, List<String> skippedMessages) {
        if (item.getExtId() != null) {
            Optional<Question> existing = questionRepository.findByExternalId(item.getExtId());

            if (existing.isPresent()) {
                String msg = ITEM_PREFIX + index + ": skipped — extId " + item.getExtId() + " already exists";
                log.debug(msg);
                skippedMessages.add(msg);

                return ItemStatus.SKIPPED;
            }
        }

        Optional<Language> languageOpt = languageRepository.findByCode(item.getLanguage());
        if (languageOpt.isEmpty()) {
            throw new IllegalArgumentException("Language not found with code: " + item.getLanguage());
        }
        Language language = languageOpt.get();

        Optional<Category> categoryOpt = categoryRepository.findByNameAndLanguageId(item.getCategory(), language.getId());
        if (categoryOpt.isEmpty()) {
            throw new IllegalArgumentException("Category '" + item.getCategory()
                    + "' not found for language: " + item.getLanguage());
        }

        List<Tag> tags = resolveOrCreateTags(item.getTags(), language);

        Question question = Question.builder()
                .externalId(item.getExtId())
                .questionText(item.getQuestion())
                .answerContent(item.getAnswer())
                .language(language)
                .category(categoryOpt.get())
                .tags(tags)
                .build();

        Question saved = questionRepository.save(question);
        questionIndexService.index(saved);

        return ItemStatus.SAVED;
    }

    private List<Tag> resolveOrCreateTags(List<String> tagNames, Language language) {
        if (tagNames == null || tagNames.isEmpty()) {
            return new ArrayList<>();
        }

        List<Tag> result = new ArrayList<>();
        for (String name : tagNames) {
            if (name == null || name.isBlank()) {
                continue;
            }
            String trimmed = name.trim();
            Tag tag = tagRepository.findByNameIgnoreCaseAndLanguageId(trimmed, language.getId())
                    .orElseGet(() -> tagRepository.save(
                            Tag.builder().name(trimmed).language(language).build()));
            result.add(tag);
        }

        return result;
    }
}
