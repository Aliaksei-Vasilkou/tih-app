package com.tih.app.repository;

import com.tih.app.model.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LanguageRepository extends JpaRepository<Language, Long> {

    Optional<Language> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByName(String name);
}
