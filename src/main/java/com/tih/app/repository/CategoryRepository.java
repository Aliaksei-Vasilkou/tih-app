package com.tih.app.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tih.app.model.Category;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("""
            SELECT c FROM Category c
            WHERE c.language.id IN :languageIds
            ORDER BY CASE WHEN c.language.id = :primaryLanguageId THEN 0 ELSE 1 END, c.id
            """)
    List<Category> findAllByLanguageIdInOrderByPrimary(
            @Param("languageIds") Collection<Long> languageIds,
            @Param("primaryLanguageId") Long primaryLanguageId);

    Optional<Category> findByNameAndLanguageId(String name, Long languageId);

    boolean existsByNameAndLanguageId(String name, Long languageId);
}
