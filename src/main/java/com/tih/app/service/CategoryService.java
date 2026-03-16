package com.tih.app.service;

import com.tih.app.dto.CategoryCreateRequest;
import com.tih.app.dto.CategoryDto;
import com.tih.app.exception.DuplicateResourceException;
import com.tih.app.exception.ResourceNotFoundException;
import com.tih.app.mapper.CategoryMapper;
import com.tih.app.model.Category;
import com.tih.app.model.Language;
import com.tih.app.repository.CategoryRepository;
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
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final LanguageRepository languageRepository;
    private final CategoryMapper categoryMapper;

    @Cacheable(value = "categories")
    public List<CategoryDto> findAll() {
        return categoryMapper.toDtoList(categoryRepository.findAllByActiveTrue());
    }

    @Cacheable(value = "categoriesByLanguage", key = "#languageId")
    public List<CategoryDto> findByLanguage(Long languageId) {
        return categoryMapper.toDtoList(categoryRepository.findAllByLanguageIdAndActiveTrue(languageId));
    }

    public CategoryDto findById(Long id) {
        return categoryMapper.toDto(getCategoryOrThrow(id));
    }

    @Transactional
    @CacheEvict(value = {"categories", "categoriesByLanguage"}, allEntries = true)
    public CategoryDto create(CategoryCreateRequest request) {
        Language language = languageRepository.findById(request.getLanguageId())
                .orElseThrow(() -> new ResourceNotFoundException("Language", request.getLanguageId()));
        if (categoryRepository.existsByNameAndLanguageId(request.getName(), request.getLanguageId())) {
            throw new DuplicateResourceException("Category", "name", request.getName());
        }
        Category category = categoryMapper.toEntity(request);
        category.setLanguage(language);
        category.setActive(true);
        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = {"categories", "categoriesByLanguage"}, allEntries = true)
    public CategoryDto update(Long id, CategoryCreateRequest request) {
        Category category = getCategoryOrThrow(id);
        Language language = languageRepository.findById(request.getLanguageId())
                .orElseThrow(() -> new ResourceNotFoundException("Language", request.getLanguageId()));
        categoryMapper.updateEntity(request, category);
        category.setLanguage(language);
        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = {"categories", "categoriesByLanguage"}, allEntries = true)
    public void delete(Long id) {
        Category category = getCategoryOrThrow(id);
        category.setActive(false);
        categoryRepository.save(category);
        log.info("Soft-deleted category with id: {}", id);
    }

    private Category getCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }
}
