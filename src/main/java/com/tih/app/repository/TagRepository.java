package com.tih.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tih.app.model.Tag;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findAllByLanguageId(Long languageId);

    Optional<Tag> findByNameIgnoreCaseAndLanguageId(String name, Long languageId);

    boolean existsByNameIgnoreCaseAndLanguageId(String name, Long languageId);

    List<Tag> findAllByLanguageIdAndNameIgnoreCaseIn(Long languageId, List<String> names);
}
