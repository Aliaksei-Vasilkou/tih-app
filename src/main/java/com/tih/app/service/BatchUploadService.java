package com.tih.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tih.app.dto.BatchUploadItem;
import com.tih.app.dto.BatchUploadResponse;
import com.tih.app.dto.QuestionExportItem;
import com.tih.app.model.Category;
import com.tih.app.model.Language;
import com.tih.app.model.Question;
import com.tih.app.repository.CategoryRepository;
import com.tih.app.repository.LanguageRepository;
import com.tih.app.repository.QuestionRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchUploadService {

    private enum ItemStatus { SAVED, SKIPPED, FAILED }

    private final QuestionRepository questionRepository;
    private final LanguageRepository languageRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final QuestionIndexService questionIndexService;

    // ------------------------------------------------------------------ import

    @Transactional
    public BatchUploadResponse processUpload(MultipartFile file) {
        List<BatchUploadItem> items;
        try {
            items = objectMapper.readValue(file.getInputStream(),
                    new TypeReference<List<BatchUploadItem>>() {});
        } catch (Exception e) {
            log.error("Failed to parse upload file", e);
            return BatchUploadResponse.builder()
                    .totalItems(0)
                    .successCount(0)
                    .failureCount(0)
                    .skippedCount(0)
                    .errors(List.of("Failed to parse file: " + e.getMessage()))
                    .skipped(List.of())
                    .build();
        }

        int success = 0, failure = 0, skipped = 0;
        List<String> errors  = new ArrayList<>();
        List<String> skippedMessages = new ArrayList<>();

        for (int i = 0; i < items.size(); i++) {
            BatchUploadItem item = items.get(i);
            try {
                Set<ConstraintViolation<BatchUploadItem>> violations = validator.validate(item);
                if (!violations.isEmpty()) {
                    String msg = "Item " + (i + 1) + ": " +
                            violations.stream().map(ConstraintViolation::getMessage)
                                      .reduce((a, b) -> a + "; " + b).orElse("");
                    errors.add(msg);
                    failure++;
                    continue;
                }

                ItemStatus status = processItem(item, i + 1, skippedMessages);
                switch (status) {
                    case SAVED   -> success++;
                    case SKIPPED -> skipped++;
                    case FAILED  -> failure++;
                }
            } catch (Exception e) {
                log.warn("Failed to process item {}: {}", i + 1, e.getMessage());
                errors.add("Item " + (i + 1) + ": " + e.getMessage());
                failure++;
            }
        }

        log.info("Batch upload completed. Saved: {}, Skipped: {}, Failed: {}", success, skipped, failure);
        return BatchUploadResponse.builder()
                .totalItems(items.size())
                .successCount(success)
                .failureCount(failure)
                .skippedCount(skipped)
                .errors(errors)
                .skipped(skippedMessages)
                .build();
    }

    private ItemStatus processItem(BatchUploadItem item, int index, List<String> skippedMessages) {
        // Deduplication check: if a UUID is supplied and already exists → skip
        if (item.getExternalId() != null) {
            Optional<Question> existing = questionRepository.findByExternalId(item.getExternalId());
            if (existing.isPresent()) {
                String msg = "Item " + index + ": skipped — externalId " + item.getExternalId() + " already exists";
                log.debug(msg);
                skippedMessages.add(msg);
                return ItemStatus.SKIPPED;
            }
        }

        Optional<Language> languageOpt = languageRepository.findByCode(item.getLanguageCode());
        if (languageOpt.isEmpty()) {
            throw new IllegalArgumentException("Language not found with code: " + item.getLanguageCode());
        }
        Language language = languageOpt.get();

        Optional<Category> categoryOpt = categoryRepository
                .findByNameAndLanguageId(item.getCategoryName(), language.getId());
        if (categoryOpt.isEmpty()) {
            throw new IllegalArgumentException("Category '" + item.getCategoryName()
                    + "' not found for language: " + item.getLanguageCode());
        }

        Question question = Question.builder()
                .externalId(item.getExternalId())   // null → @PrePersist will generate one
                .questionText(item.getQuestionText())
                .answerContent(item.getAnswerContent())
                .language(language)
                .category(categoryOpt.get())
                .build();

        Question saved = questionRepository.save(question);
        questionIndexService.index(saved);
        return ItemStatus.SAVED;
    }

    // ------------------------------------------------------------------ export

    @Transactional(readOnly = true)
    public List<QuestionExportItem> exportQuestions(String languageCode, String categoryName) {
        List<Question> questions = questionRepository.findAllForExport(
                languageCode != null && !languageCode.isBlank() ? languageCode : null,
                categoryName != null && !categoryName.isBlank() ? categoryName : null);

        log.info("Exporting {} question(s) [languageCode={}, categoryName={}]",
                questions.size(), languageCode, categoryName);

        return questions.stream()
                .map(q -> QuestionExportItem.builder()
                        .externalId(q.getExternalId())
                        .questionText(q.getQuestionText())
                        .answerContent(q.getAnswerContent())
                        .languageCode(q.getLanguage().getCode())
                        .categoryName(q.getCategory().getName())
                        .build())
                .toList();
    }
}
