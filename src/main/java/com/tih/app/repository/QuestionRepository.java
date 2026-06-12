package com.tih.app.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tih.app.model.Question;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    Optional<Question> findByExternalId(UUID externalId);

    /**
     * Browse query filtered by a set of language IDs, optional category, and optional level filter.
     * When {@code levelFilterFlag} is null the level clauses are skipped and all questions are returned.
     * {@code includedLevels} and {@code allLevelTags} must always be non-empty collections.
     */
    @Query("""
            SELECT DISTINCT q FROM Question q
            WHERE q.language.id IN :languageIds
              AND (:categoryId IS NULL OR q.category.id = :categoryId)
              AND (:levelFilterFlag IS NULL
                   OR EXISTS (SELECT t FROM q.tags t WHERE t.name IN :includedLevels)
                   OR NOT EXISTS (SELECT t FROM q.tags t WHERE t.name IN :allLevelTags))
            ORDER BY q.createdAt DESC
            """)
    Page<Question> findAllByLanguageIdsAndFilters(
            @Param("languageIds") Collection<Long> languageIds,
            @Param("categoryId") Long categoryId,
            @Param("levelFilterFlag") String levelFilterFlag,
            @Param("includedLevels") Collection<String> includedLevels,
            @Param("allLevelTags") Collection<String> allLevelTags,
            Pageable pageable);

    /**
     * Browse query with optional category and level filters (no language filter).
     * When {@code levelFilterFlag} is null the level clauses are skipped and all questions are returned.
     * {@code includedLevels} and {@code allLevelTags} must always be non-empty collections.
     */
    @Query("""
            SELECT DISTINCT q FROM Question q
            WHERE (:categoryId IS NULL OR q.category.id = :categoryId)
              AND (:levelFilterFlag IS NULL
                   OR EXISTS (SELECT t FROM q.tags t WHERE t.name IN :includedLevels)
                   OR NOT EXISTS (SELECT t FROM q.tags t WHERE t.name IN :allLevelTags))
            ORDER BY q.createdAt DESC
            """)
    Page<Question> findAllByFilters(
            @Param("categoryId") Long categoryId,
            @Param("levelFilterFlag") String levelFilterFlag,
            @Param("includedLevels") Collection<String> includedLevels,
            @Param("allLevelTags") Collection<String> allLevelTags,
            Pageable pageable);

    /**
     * Fetches all questions for export, eagerly joining language, category and tags
     * to avoid N+1. Both filter params are optional (pass null to skip the filter).
     */
    @Query("""
            SELECT DISTINCT q FROM Question q
            JOIN FETCH q.language l
            JOIN FETCH q.category c
            LEFT JOIN FETCH q.tags
            WHERE (:languageCode IS NULL OR l.code = :languageCode)
              AND (:categoryName IS NULL OR c.name = :categoryName)
            ORDER BY l.code, c.name, q.id
            """)
    List<Question> findAllForExport(
            @Param("languageCode") String languageCode,
            @Param("categoryName") String categoryName);

    Page<Question> findAllByLanguageIdAndCategoryId(Long languageId, Long categoryId, Pageable pageable);

    Page<Question> findAllByLanguageId(Long languageId, Pageable pageable);

    /**
     * Filters by a set of language IDs (used to include General + selected language).
     */
    Page<Question> findAllByLanguageIdIn(Collection<Long> languageIds, Pageable pageable);

    /**
     * Filters by a set of language IDs AND a category (used to include General + selected language).
     */
    Page<Question> findAllByLanguageIdInAndCategoryId(Collection<Long> languageIds, Long categoryId, Pageable pageable);

    Page<Question> findAllByCategoryId(Long categoryId, Pageable pageable);

    /**
     * Full-text search using PostgreSQL tsvector.
     * Returns results ordered by relevance rank.
     */
    @Query(value = """
            SELECT q.* FROM questions q
            WHERE q.search_vector @@ plainto_tsquery('english', :query)
              AND (:languageId IS NULL OR q.language_id = :languageId)
              AND (:categoryId IS NULL OR q.category_id = :categoryId)
              AND (:levelFilterFlag IS NULL
                   OR EXISTS (SELECT 1 FROM question_tags qt JOIN tags t ON qt.tag_id = t.id
                              WHERE qt.question_id = q.id AND t.name IN (:includedLevels))
                   OR NOT EXISTS (SELECT 1 FROM question_tags qt JOIN tags t ON qt.tag_id = t.id
                                  WHERE qt.question_id = q.id AND t.name IN (:allLevelTags)))
            ORDER BY ts_rank(q.search_vector, plainto_tsquery('english', :query)) DESC
            """,
            countQuery = """
                    SELECT count(*) FROM questions q
                    WHERE q.search_vector @@ plainto_tsquery('english', :query)
                      AND (:languageId IS NULL OR q.language_id = :languageId)
                      AND (:categoryId IS NULL OR q.category_id = :categoryId)
                      AND (:levelFilterFlag IS NULL
                           OR EXISTS (SELECT 1 FROM question_tags qt JOIN tags t ON qt.tag_id = t.id
                                      WHERE qt.question_id = q.id AND t.name IN (:includedLevels))
                           OR NOT EXISTS (SELECT 1 FROM question_tags qt JOIN tags t ON qt.tag_id = t.id
                                          WHERE qt.question_id = q.id AND t.name IN (:allLevelTags)))
                    """,
            nativeQuery = true)
    Page<Question> searchByFullText(
            @Param("query") String query,
            @Param("languageId") Long languageId,
            @Param("categoryId") Long categoryId,
            @Param("levelFilterFlag") String levelFilterFlag,
            @Param("includedLevels") Collection<String> includedLevels,
            @Param("allLevelTags") Collection<String> allLevelTags,
            Pageable pageable);

    /**
     * Full-text search filtered by a set of language IDs (e.g. selected language + General).
     */
    @Query(value = """
            SELECT q.* FROM questions q
            WHERE q.search_vector @@ plainto_tsquery('english', :query)
              AND q.language_id IN (:languageIds)
              AND (:categoryId IS NULL OR q.category_id = :categoryId)
              AND (:levelFilterFlag IS NULL
                   OR EXISTS (SELECT 1 FROM question_tags qt JOIN tags t ON qt.tag_id = t.id
                              WHERE qt.question_id = q.id AND t.name IN (:includedLevels))
                   OR NOT EXISTS (SELECT 1 FROM question_tags qt JOIN tags t ON qt.tag_id = t.id
                                  WHERE qt.question_id = q.id AND t.name IN (:allLevelTags)))
            ORDER BY ts_rank(q.search_vector, plainto_tsquery('english', :query)) DESC
            """,
            countQuery = """
                    SELECT count(*) FROM questions q
                    WHERE q.search_vector @@ plainto_tsquery('english', :query)
                      AND q.language_id IN (:languageIds)
                      AND (:categoryId IS NULL OR q.category_id = :categoryId)
                      AND (:levelFilterFlag IS NULL
                           OR EXISTS (SELECT 1 FROM question_tags qt JOIN tags t ON qt.tag_id = t.id
                                      WHERE qt.question_id = q.id AND t.name IN (:includedLevels))
                           OR NOT EXISTS (SELECT 1 FROM question_tags qt JOIN tags t ON qt.tag_id = t.id
                                          WHERE qt.question_id = q.id AND t.name IN (:allLevelTags)))
                    """,
            nativeQuery = true)
    Page<Question> searchByFullTextWithLanguageIds(
            @Param("query") String query,
            @Param("languageIds") Collection<Long> languageIds,
            @Param("categoryId") Long categoryId,
            @Param("levelFilterFlag") String levelFilterFlag,
            @Param("includedLevels") Collection<String> includedLevels,
            @Param("allLevelTags") Collection<String> allLevelTags,
            Pageable pageable);

    /**
     * Fallback ILIKE search when full-text search query is too short.
     * Results are ordered so that question-title matches surface before answer-only matches.
     */
    @Query("""
            SELECT q FROM Question q
            WHERE (LOWER(q.questionText) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(q.answerContent) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:languageId IS NULL OR q.language.id = :languageId)
              AND (:categoryId IS NULL OR q.category.id = :categoryId)
              AND (:levelFilterFlag IS NULL
                   OR EXISTS (SELECT t FROM q.tags t WHERE t.name IN :includedLevels)
                   OR NOT EXISTS (SELECT t FROM q.tags t WHERE t.name IN :allLevelTags))
            ORDER BY
                CASE WHEN LOWER(q.questionText) LIKE LOWER(CONCAT('%', :query, '%')) THEN 0 ELSE 1 END,
                q.id
            """)
    Page<Question> searchByKeyword(
            @Param("query") String query,
            @Param("languageId") Long languageId,
            @Param("categoryId") Long categoryId,
            @Param("levelFilterFlag") String levelFilterFlag,
            @Param("includedLevels") Collection<String> includedLevels,
            @Param("allLevelTags") Collection<String> allLevelTags,
            Pageable pageable);

    /**
     * Fallback ILIKE search filtered by a set of language IDs (e.g. selected language + General).
     * Results are ordered so that question-title matches surface before answer-only matches.
     */
    @Query("""
            SELECT q FROM Question q
            WHERE (LOWER(q.questionText) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(q.answerContent) LIKE LOWER(CONCAT('%', :query, '%')))
              AND q.language.id IN (:languageIds)
              AND (:categoryId IS NULL OR q.category.id = :categoryId)
              AND (:levelFilterFlag IS NULL
                   OR EXISTS (SELECT t FROM q.tags t WHERE t.name IN :includedLevels)
                   OR NOT EXISTS (SELECT t FROM q.tags t WHERE t.name IN :allLevelTags))
            ORDER BY
                CASE WHEN LOWER(q.questionText) LIKE LOWER(CONCAT('%', :query, '%')) THEN 0 ELSE 1 END,
                q.id
            """)
    Page<Question> searchByKeywordWithLanguageIds(
            @Param("query") String query,
            @Param("languageIds") Collection<Long> languageIds,
            @Param("categoryId") Long categoryId,
            @Param("levelFilterFlag") String levelFilterFlag,
            @Param("includedLevels") Collection<String> includedLevels,
            @Param("allLevelTags") Collection<String> allLevelTags,
            Pageable pageable);
}
