package com.tih.app.service;

import com.tih.app.dto.PageResponse;
import com.tih.app.dto.QuestionCreateRequest;
import com.tih.app.dto.QuestionDto;
import com.tih.app.dto.QuestionSearchRequest;
import com.tih.app.exception.ResourceNotFoundException;
import com.tih.app.mapper.QuestionMapper;
import com.tih.app.model.Category;
import com.tih.app.model.Language;
import com.tih.app.model.Question;
import com.tih.app.model.Tag;
import com.tih.app.repository.CategoryRepository;
import com.tih.app.repository.LanguageRepository;
import com.tih.app.repository.QuestionRepository;
import com.tih.app.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class QuestionService {

    private static final int MIN_FTS_QUERY_LENGTH = 3;

    private final QuestionRepository questionRepository;
    private final LanguageRepository languageRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final QuestionMapper questionMapper;
    private final QuestionIndexService questionIndexService;
    private final QuestionSearchService questionSearchService;

    public PageResponse<QuestionDto> findAll(Long languageId, Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Question> result;
        if (languageId != null && categoryId != null) {
            result = questionRepository.findAllByLanguageIdAndCategoryId(languageId, categoryId, pageable);
        } else if (languageId != null) {
            result = questionRepository.findAllByLanguageId(languageId, pageable);
        } else if (categoryId != null) {
            result = questionRepository.findAllByCategoryId(categoryId, pageable);
        } else {
            result = questionRepository.findAll(pageable);
        }
        return toPageResponse(result);
    }

    @Cacheable(value = "questions", key = "#id")
    public QuestionDto findById(Long id) {
        return questionMapper.toDto(getQuestionOrThrow(id));
    }

    public PageResponse<QuestionDto> search(QuestionSearchRequest searchRequest) {
        String query = searchRequest.getQuery();
        Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize());

        if (!StringUtils.hasText(query)) {
            return findAll(searchRequest.getLanguageId(), searchRequest.getCategoryId(),
                    searchRequest.getPage(), searchRequest.getSize());
        }

        try {
            return questionSearchService.search(
                    query.trim(), searchRequest.getLanguageId(), searchRequest.getCategoryId(), pageable);
        } catch (Exception e) {
            log.warn("Elasticsearch search unavailable ({}), falling back to PostgreSQL FTS", e.getMessage());
            return fallbackSearch(query.trim(), searchRequest.getLanguageId(),
                    searchRequest.getCategoryId(), pageable);
        }
    }

    @Transactional
    @CacheEvict(value = "questions", allEntries = true)
    public QuestionDto create(QuestionCreateRequest request) {
        Language language = languageRepository.findById(request.getLanguageId())
                .orElseThrow(() -> new ResourceNotFoundException("Language", request.getLanguageId()));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
        Question question = questionMapper.toEntity(request);
        question.setLanguage(language);
        question.setCategory(category);
        question.setTags(resolveTags(request.getTagIds()));
        Question saved = questionRepository.save(question);
        questionIndexService.index(saved);
        return questionMapper.toDto(saved);
    }

    @Transactional
    @CacheEvict(value = "questions", key = "#id")
    public QuestionDto update(Long id, QuestionCreateRequest request) {
        Question question = getQuestionOrThrow(id);
        Language language = languageRepository.findById(request.getLanguageId())
                .orElseThrow(() -> new ResourceNotFoundException("Language", request.getLanguageId()));
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
        questionMapper.updateEntity(request, question);
        question.setLanguage(language);
        question.setCategory(category);
        question.getTags().clear();
        question.getTags().addAll(resolveTags(request.getTagIds()));
        Question saved = questionRepository.save(question);
        questionIndexService.index(saved);
        return questionMapper.toDto(saved);
    }

    @Transactional
    @CacheEvict(value = "questions", key = "#id")
    public void delete(Long id) {
        getQuestionOrThrow(id);
        questionRepository.deleteById(id);
        questionIndexService.delete(id);
        log.info("Deleted question with id: {}", id);
    }

    // ------------------------------------------------------------------ helpers

    private PageResponse<QuestionDto> fallbackSearch(String query, Long languageId, Long categoryId, Pageable pageable) {
        Page<Question> result;
        if (query.length() >= MIN_FTS_QUERY_LENGTH) {
            result = questionRepository.searchByFullText(query, languageId, categoryId, pageable);
        } else {
            result = questionRepository.searchByKeyword(query, languageId, categoryId, pageable);
        }
        return toPageResponse(result);
    }

    private Question getQuestionOrThrow(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question", id));
    }

    private List<Tag> resolveTags(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(tagRepository.findAllById(tagIds));
    }

    private PageResponse<QuestionDto> toPageResponse(Page<Question> page) {
        return PageResponse.<QuestionDto>builder()
                .content(questionMapper.toDtoList(page.getContent()))
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
