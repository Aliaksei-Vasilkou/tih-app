package com.tih.app.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final LanguageRepository languageRepository;
    private final CategoryMapper categoryMapper;
    private final LanguageService languageService;

    @Cacheable(value = "categories")
    public List<CategoryDto> findAll() {
        return categoryMapper.toDtoList(categoryRepository.findAll());
    }

    @Cacheable(value = "categoriesByLanguage", key = "#languageId")
    public List<CategoryDto> findByLanguage(Long languageId) {
        List<Long> languageIds = languageService.resolveLanguageIds(languageId);

        return categoryMapper.toDtoList(categoryRepository.findAllByLanguageIdInOrderByPrimary(languageIds, languageId));
    }

    public CategoryDto findById(Long id) {
        return categoryMapper.toDto(getCategoryOrThrow(id));
    }

    @Transactional
    @CacheEvict(value = { "categories", "categoriesByLanguage" }, allEntries = true)
    public CategoryDto create(CategoryCreateRequest request) {
        Language language = languageRepository.findById(request.languageId())
                .orElseThrow(() -> new ResourceNotFoundException("Language", request.languageId()));

        if (categoryRepository.existsByNameAndLanguageId(request.name(), request.languageId())) {
            throw new DuplicateResourceException("Category", "name", request.name());
        }

        Category category = categoryMapper.toEntity(request);
        category.setLanguage(language);

        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = { "categories", "categoriesByLanguage" }, allEntries = true)
    public CategoryDto update(Long id, CategoryCreateRequest request) {
        Category category = getCategoryOrThrow(id);
        Language language = languageRepository.findById(request.languageId())
                .orElseThrow(() -> new ResourceNotFoundException("Language", request.languageId()));
        categoryMapper.updateEntity(request, category);
        category.setLanguage(language);

        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = { "categories", "categoriesByLanguage" }, allEntries = true)
    public void delete(Long id) {
        getCategoryOrThrow(id);
        categoryRepository.deleteById(id);
        log.info("Deleted category with id: {}", id);
    }

    private Category getCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }
}
