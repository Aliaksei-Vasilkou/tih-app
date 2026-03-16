package com.tih.app.repository;

import com.tih.app.model.Question;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    Page<Question> findAllByActiveTrue(Pageable pageable);

    Page<Question> findAllByLanguageIdAndActiveTrue(Long languageId, Pageable pageable);

    Page<Question> findAllByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

    Page<Question> findAllByLanguageIdAndCategoryIdAndActiveTrue(Long languageId, Long categoryId, Pageable pageable);

    /**
     * Full-text search using PostgreSQL tsvector.
     * Returns results ordered by relevance rank.
     */
    @Query(value = """
            SELECT q.* FROM questions q
            WHERE q.active = true
              AND q.search_vector @@ plainto_tsquery('english', :query)
              AND (:languageId IS NULL OR q.language_id = :languageId)
              AND (:categoryId IS NULL OR q.category_id = :categoryId)
            ORDER BY ts_rank(q.search_vector, plainto_tsquery('english', :query)) DESC
            """,
           countQuery = """
            SELECT count(*) FROM questions q
            WHERE q.active = true
              AND q.search_vector @@ plainto_tsquery('english', :query)
              AND (:languageId IS NULL OR q.language_id = :languageId)
              AND (:categoryId IS NULL OR q.category_id = :categoryId)
            """,
           nativeQuery = true)
    Page<Question> searchByFullText(
            @Param("query") String query,
            @Param("languageId") Long languageId,
            @Param("categoryId") Long categoryId,
            Pageable pageable);

    /**
     * Fallback ILIKE search when full-text search query is too short.
     */
    @Query("""
            SELECT q FROM Question q
            WHERE q.active = true
              AND (LOWER(q.questionText) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(q.answerContent) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:languageId IS NULL OR q.language.id = :languageId)
              AND (:categoryId IS NULL OR q.category.id = :categoryId)
            """)
    Page<Question> searchByKeyword(
            @Param("query") String query,
            @Param("languageId") Long languageId,
            @Param("categoryId") Long categoryId,
            Pageable pageable);
}
