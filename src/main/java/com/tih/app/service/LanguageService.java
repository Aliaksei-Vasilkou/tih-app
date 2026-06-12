package com.tih.app.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tih.app.dto.LanguageCreateRequest;
import com.tih.app.dto.LanguageDto;
import com.tih.app.exception.DuplicateResourceException;
import com.tih.app.exception.ResourceNotFoundException;
import com.tih.app.mapper.LanguageMapper;
import com.tih.app.model.Language;
import com.tih.app.repository.LanguageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LanguageService {

    private static final String GENERAL_LANGUAGE_CODE = "general";

    private final LanguageRepository languageRepository;
    private final LanguageMapper languageMapper;

    // Cached after first lookup — avoids repeated DB round-trips for every filtered query.
    private volatile Long generalLanguageId;

    @Cacheable("languages")
    public List<LanguageDto> findAll() {
        log.debug("Fetching all languages");
        return languageMapper.toDtoList(languageRepository.findAll());
    }

    public LanguageDto findById(Long id) {
        return languageMapper.toDto(getLanguageOrThrow(id));
    }

    @Transactional
    @CacheEvict(value = "languages", allEntries = true)
    public LanguageDto create(LanguageCreateRequest request) {
        if (languageRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Language", "code", request.code());
        }

        if (languageRepository.existsByName(request.name())) {
            throw new DuplicateResourceException("Language", "name", request.name());
        }

        Language language = languageMapper.toEntity(request);

        return languageMapper.toDto(languageRepository.save(language));
    }

    @Transactional
    @CacheEvict(value = "languages", allEntries = true)
    public LanguageDto update(Long id, LanguageCreateRequest request) {
        Language language = getLanguageOrThrow(id);
        languageMapper.updateEntity(request, language);

        return languageMapper.toDto(languageRepository.save(language));
    }

    @Transactional
    @CacheEvict(value = "languages", allEntries = true)
    public void delete(Long id) {
        getLanguageOrThrow(id);
        languageRepository.deleteById(id);
        log.info("Deleted language with id: {}", id);
    }

    /**
     * Resolves the set of language IDs to filter by, always including the general language.
     * Returns {@code null} when no language is selected (callers fall through to unfiltered queries).
     */
    public List<Long> resolveLanguageIds(Long requestedId) {
        if (requestedId == null) {
            return null;
        }

        Long generalId = resolveGeneralLanguageId();

        if (requestedId.equals(generalId)) {
            return List.of(generalId);
        }

        return List.of(requestedId, generalId);
    }

    private Language getLanguageOrThrow(Long id) {
        return languageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Language", id));
    }

    private Long resolveGeneralLanguageId() {
        if (generalLanguageId == null) {
            generalLanguageId = languageRepository.findByCode(GENERAL_LANGUAGE_CODE)
                    .orElseThrow(() -> new ResourceNotFoundException("Language", "code", GENERAL_LANGUAGE_CODE))
                    .getId();
        }

        return generalLanguageId;
    }
}
