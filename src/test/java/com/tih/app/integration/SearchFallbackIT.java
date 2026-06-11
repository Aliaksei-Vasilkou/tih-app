package com.tih.app.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.tih.app.config.AuditConfig;
import com.tih.app.model.Category;
import com.tih.app.model.Language;
import com.tih.app.model.Question;
import com.tih.app.repository.CategoryRepository;
import com.tih.app.repository.LanguageRepository;
import com.tih.app.repository.QuestionRepository;

/**
 * Integration tests for the PostgreSQL ILIKE fallback search path.
 * <p>
 * These tests verify that {@link QuestionRepository#searchByKeyword} and
 * {@link QuestionRepository#searchByKeywordWithLanguageIds} order results so that
 * question-title matches surface before answer-only matches (T003 / TIH-053).
 * <p>
 * Uses a real PostgreSQL container (Testcontainers) via {@code @DataJpaTest} with
 * {@code replace = NONE}. Liquibase migrations run automatically, seeding languages
 * and categories. Tests reuse the seeded "general" language and "Database" category.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Import(AuditConfig.class)
class SearchFallbackIT {

    @Container
    @ServiceConnection
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    @Autowired
    QuestionRepository questionRepository;

    @Autowired
    LanguageRepository languageRepository;

    @Autowired
    CategoryRepository categoryRepository;

    private Language language;
    private Category category;

    @BeforeEach
    void setUp() {
        questionRepository.deleteAll();

        language = languageRepository.findByCode("general")
                .orElseThrow(() -> new IllegalStateException(
                        "Seed language 'general' not found — check migration 004-seed-initial-data.xml"));

        category = categoryRepository.findByNameAndLanguageId("Database", language.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Seed category 'Database' not found for language 'general' — check migration 004-seed-initial-data.xml"));
    }

    /**
     * T004 (ILIKE fallback): When searching for a short query ("gc") that uses the
     * ILIKE fallback path, the result where "gc" appears in the question title must
     * be ranked first — before a result where "gc" only appears in the answer body.
     * Validates: {@code ORDER BY CASE WHEN LOWER(q.questionText) LIKE ... THEN 0 ELSE 1 END} in
     * {@link QuestionRepository#searchByKeyword}.
     */
    @Test
    void searchByKeyword_titleMatch_ranksBeforeAnswerOnlyMatch() {
        // given
        Question titleMatch = save("What is GC in the JVM?",
                "Garbage collection frees objects from the heap.");
        save("How does JVM manage heap?",
                "The JVM employs GC (garbage collection) to reclaim unused memory objects.");

        // when
        Page<Question> results = questionRepository.searchByKeyword(
                "gc", null, null, PageRequest.of(0, 10));

        // then
        assertThat(results.getContent()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(results.getContent().getFirst().getId())
                .as("Title-match question should be ranked first")
                .isEqualTo(titleMatch.getId());
    }

    /**
     * T004 (ILIKE fallback, language-filtered): Same ordering assertion but using the
     * language-filtered variant of the fallback query.
     * Validates: {@code ORDER BY} in {@link QuestionRepository#searchByKeywordWithLanguageIds}.
     */
    @Test
    void searchByKeywordWithLanguageIds_titleMatch_ranksBeforeAnswerOnlyMatch() {
        // given
        Question titleMatch = save("GC tuning in Java",
                "You can tune GC with JVM flags like -Xmx.");
        save("JVM performance overview",
                "Good GC settings are essential for low-latency Java applications.");

        // when
        Page<Question> results = questionRepository.searchByKeywordWithLanguageIds(
                "gc", List.of(language.getId()), null, PageRequest.of(0, 10));

        // then
        assertThat(results.getContent()).hasSizeGreaterThanOrEqualTo(2);
        assertThat(results.getContent().getFirst().getId())
                .as("Title-match question should be ranked first in language-filtered fallback")
                .isEqualTo(titleMatch.getId());
    }

    /**
     * Sanity check: when the query matches only the title of one document, that
     * document is returned even when the answer does not contain the term.
     */
    @Test
    void searchByKeyword_returnsOnlyTitleMatch_whenAnswerDoesNotContainQuery() {
        // given
        Question titleOnly = save("What is GC in Java?",
                "The JVM manages memory automatically.");
        save("Explain JVM heap allocation",
                "The heap is divided into young and old generation.");

        // when
        Page<Question> results = questionRepository.searchByKeyword(
                "gc", null, null, PageRequest.of(0, 10));

        // then
        assertThat(results.getContent()).hasSize(1);
        assertThat(results.getContent().getFirst().getId()).isEqualTo(titleOnly.getId());
    }

    private Question save(String questionText, String answerContent) {
        return questionRepository.save(Question.builder()
                .questionText(questionText)
                .answerContent(answerContent)
                .language(language)
                .category(category)
                .build());
    }
}
