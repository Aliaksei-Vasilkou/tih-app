package com.tih.app.repository;

import com.tih.app.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByActiveTrue();

    List<Category> findAllByLanguageIdAndActiveTrue(Long languageId);

    Optional<Category> findByNameAndLanguageId(String name, Long languageId);

    boolean existsByNameAndLanguageId(String name, Long languageId);
}
