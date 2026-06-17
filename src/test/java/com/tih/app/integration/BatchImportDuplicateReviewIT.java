package com.tih.app.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tih.app.config.AuditConfig;
import com.tih.app.dto.BatchAnalyseResponse;
import com.tih.app.dto.BatchCommitRequest;
import com.tih.app.dto.BatchUploadResponse;
import com.tih.app.dto.ConflictResolution;
import com.tih.app.dto.QuestionTransferItem;
import com.tih.app.mapper.QuestionMapperImpl;
import com.tih.app.model.Category;
import com.tih.app.model.Language;
import com.tih.app.model.Question;
import com.tih.app.repository.CategoryRepository;
import com.tih.app.repository.LanguageRepository;
import com.tih.app.repository.QuestionRepository;
import com.tih.app.repository.TagRepository;
import com.tih.app.service.BatchUploadService;
import com.tih.app.service.QuestionIndexService;

import jakarta.validation.Validation;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(AuditConfig.class)
class BatchImportDuplicateReviewIT {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private LanguageRepository languageRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    private BatchUploadService service;
    private Language javaLanguage;
    private Category coreCategory;

    @BeforeEach
    void setUp() {
        questionRepository.deleteAll();

        javaLanguage = languageRepository.findByCode("java")
                .orElseThrow(() -> new IllegalStateException("Seed language 'java' not found"));

        coreCategory = categoryRepository.findByNameAndLanguageId("Core", javaLanguage.getId())
                .orElseThrow(() -> new IllegalStateException("Seed category 'Core' not found for java"));

        service = new BatchUploadService(
                questionRepository,
                languageRepository,
                categoryRepository,
                tagRepository,
                new ObjectMapper(),
                Validation.buildDefaultValidatorFactory().getValidator(),
                mock(QuestionIndexService.class),
                new QuestionMapperImpl());
    }

    @Test
    void analyseUpload_mixedItems_doesNotPersistData() {
        // given
        UUID duplicateExtId = UUID.randomUUID();
        questionRepository.save(Question.builder()
                .externalId(duplicateExtId)
                .questionText("Existing duplicate")
                .answerContent("Existing answer")
                .language(javaLanguage)
                .category(coreCategory)
                .build());
        long beforeCount = questionRepository.count();
        String payload = """
                [
                  {
                    \"question\": \"What is polymorphism?\",
                    \"answer\": \"## Polymorphism\",
                    \"language\": \"java\",
                    \"category\": \"Core\",
                    \"tags\": [\"L1\", \"OOP\"]
                  },
                  {
                    \"extId\": \"%s\",
                    \"question\": \"What is inheritance?\",
                    \"answer\": \"## Inheritance\",
                    \"language\": \"java\",
                    \"category\": \"Core\",
                    \"tags\": [\"L1\"]
                  }
                ]
                """.formatted(duplicateExtId);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "analyse.json",
                "application/json",
                payload.getBytes(StandardCharsets.UTF_8));

        // when
        BatchAnalyseResponse response = service.analyseUpload(file);

        // then
        assertThat(response.newItems()).hasSize(1);
        assertThat(response.duplicates()).hasSize(1);
        assertThat(response.duplicates().getFirst().extId()).isEqualTo(duplicateExtId);
        assertThat(questionRepository.count()).isEqualTo(beforeCount);
    }

    @Test
    void commitUpload_acceptAndSkip_appliesExpectedCountersAndUpdates() {
        // given
        UUID acceptedExtId = UUID.randomUUID();
        UUID skippedExtId = UUID.randomUUID();
        questionRepository.save(Question.builder()
                .externalId(acceptedExtId)
                .questionText("Old accepted")
                .answerContent("Old answer")
                .language(javaLanguage)
                .category(coreCategory)
                .build());
        questionRepository.save(Question.builder()
                .externalId(skippedExtId)
                .questionText("Old skipped")
                .answerContent("Old skipped answer")
                .language(javaLanguage)
                .category(coreCategory)
                .build());

        QuestionTransferItem newItem = QuestionTransferItem.builder()
                .question("What is abstraction?")
                .answer("## Abstraction")
                .language("java")
                .category("Core")
                .tags(List.of("L1"))
                .build();
        QuestionTransferItem incomingAccepted = QuestionTransferItem.builder()
                .extId(acceptedExtId)
                .question("What is inheritance?")
                .answer("## Updated inheritance answer")
                .language("java")
                .category("Core")
                .tags(List.of("L2"))
                .build();
        BatchCommitRequest request = new BatchCommitRequest(
                List.of(newItem, incomingAccepted),
                List.of(
                        new ConflictResolution(acceptedExtId, "accept"),
                        new ConflictResolution(skippedExtId, "skip")));

        // when
        BatchUploadResponse response = service.commitUpload(request);

        // then
        Question updatedAccepted = questionRepository.findByExternalId(acceptedExtId).orElseThrow();
        Question unchangedSkipped = questionRepository.findByExternalId(skippedExtId).orElseThrow();

        assertThat(response.getSuccessCount()).isEqualTo(1);
        assertThat(response.getUpdatedCount()).isEqualTo(1);
        assertThat(response.getSkippedCount()).isEqualTo(1);
        assertThat(response.getFailureCount()).isZero();
        assertThat(response.getSkipped()).containsExactly(skippedExtId.toString());
        assertThat(updatedAccepted.getQuestionText()).isEqualTo("What is inheritance?");
        assertThat(updatedAccepted.getAnswerContent()).isEqualTo("## Updated inheritance answer");
        assertThat(unchangedSkipped.getQuestionText()).isEqualTo("Old skipped");
    }
}
