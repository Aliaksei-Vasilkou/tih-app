package com.tih.app.service;

import com.tih.app.dto.LanguageCreateRequest;
import com.tih.app.dto.LanguageDto;
import com.tih.app.exception.DuplicateResourceException;
import com.tih.app.exception.ResourceNotFoundException;
import com.tih.app.mapper.LanguageMapper;
import com.tih.app.model.Language;
import com.tih.app.repository.LanguageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class LanguageService {

    private final LanguageRepository languageRepository;
    private final LanguageMapper languageMapper;

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
        if (languageRepository.existsByCode(request.getCode())) {
            throw new DuplicateResourceException("Language", "code", request.getCode());
        }
        if (languageRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Language", "name", request.getName());
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

    private Language getLanguageOrThrow(Long id) {
        return languageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Language", id));
    }
}
