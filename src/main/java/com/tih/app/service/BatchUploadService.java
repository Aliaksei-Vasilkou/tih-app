package com.tih.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tih.app.dto.BatchUploadItem;
import com.tih.app.dto.BatchUploadResponse;
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

    private final QuestionRepository questionRepository;
    private final LanguageRepository languageRepository;
    private final CategoryRepository categoryRepository;
    private final ObjectMapper objectMapper;
    private final Validator validator;

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
                    .errors(List.of("Failed to parse file: " + e.getMessage()))
                    .build();
        }

        int success = 0;
        int failure = 0;
        List<String> errors = new ArrayList<>();

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
                processItem(item, i + 1, errors);
                success++;
            } catch (Exception e) {
                log.warn("Failed to process item {}: {}", i + 1, e.getMessage());
                errors.add("Item " + (i + 1) + ": " + e.getMessage());
                failure++;
            }
        }

        log.info("Batch upload completed. Success: {}, Failure: {}", success, failure);
        return BatchUploadResponse.builder()
                .totalItems(items.size())
                .successCount(success)
                .failureCount(failure)
                .errors(errors)
                .build();
    }

    private void processItem(BatchUploadItem item, int index, List<String> errors) {
        Optional<Language> languageOpt = languageRepository.findByCodeAndActiveTrue(item.getLanguageCode());
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
                .questionText(item.getQuestionText())
                .answerContent(item.getAnswerContent())
                .language(language)
                .category(categoryOpt.get())
                .active(true)
                .build();

        questionRepository.save(question);
    }
}
