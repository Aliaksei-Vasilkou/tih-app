package com.tih.app.service;

import com.tih.app.dto.TagCreateRequest;
import com.tih.app.dto.TagDto;
import com.tih.app.exception.DuplicateResourceException;
import com.tih.app.exception.ResourceNotFoundException;
import com.tih.app.mapper.TagMapper;
import com.tih.app.model.Language;
import com.tih.app.model.Tag;
import com.tih.app.repository.LanguageRepository;
import com.tih.app.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;
    private final LanguageRepository languageRepository;
    private final TagMapper tagMapper;

    public List<TagDto> findAllByLanguageId(Long languageId) {
        validateLanguageExists(languageId);
        return tagMapper.toDtoList(tagRepository.findAllByLanguageId(languageId));
    }

    public TagDto findById(Long id) {
        return tagMapper.toDto(getTagOrThrow(id));
    }

    @Transactional
    public TagDto create(Long languageId, TagCreateRequest request) {
        Language language = validateLanguageExists(languageId);
        if (tagRepository.existsByNameIgnoreCaseAndLanguageId(request.getName(), languageId)) {
            throw new DuplicateResourceException("Tag", "name", request.getName());
        }
        Tag tag = Tag.builder()
                .name(request.getName().trim())
                .language(language)
                .build();
        return tagMapper.toDto(tagRepository.save(tag));
    }

    @Transactional
    public TagDto update(Long languageId, Long id, TagCreateRequest request) {
        validateLanguageExists(languageId);
        Tag tag = getTagOrThrow(id);
        if (!tag.getLanguage().getId().equals(languageId)) {
            throw new ResourceNotFoundException("Tag", id);
        }
        String newName = request.getName().trim();
        if (!tag.getName().equalsIgnoreCase(newName)
                && tagRepository.existsByNameIgnoreCaseAndLanguageId(newName, languageId)) {
            throw new DuplicateResourceException("Tag", "name", newName);
        }
        tag.setName(newName);
        return tagMapper.toDto(tagRepository.save(tag));
    }

    @Transactional
    public void delete(Long languageId, Long id) {
        validateLanguageExists(languageId);
        Tag tag = getTagOrThrow(id);
        if (!tag.getLanguage().getId().equals(languageId)) {
            throw new ResourceNotFoundException("Tag", id);
        }
        tagRepository.delete(tag);
        log.info("Deleted tag id={} ('{}') from language id={}", id, tag.getName(), languageId);
    }

    private Tag getTagOrThrow(Long id) {
        return tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", id));
    }

    private Language validateLanguageExists(Long languageId) {
        return languageRepository.findById(languageId)
                .orElseThrow(() -> new ResourceNotFoundException("Language", languageId));
    }
}
