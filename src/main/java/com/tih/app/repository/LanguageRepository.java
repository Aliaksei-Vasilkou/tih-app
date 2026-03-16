package com.tih.app.repository;

import com.tih.app.model.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LanguageRepository extends JpaRepository<Language, Long> {

    List<Language> findAllByActiveTrue();

    Optional<Language> findByCode(String code);

    Optional<Language> findByCodeAndActiveTrue(String code);

    boolean existsByCode(String code);

    boolean existsByName(String name);
}
