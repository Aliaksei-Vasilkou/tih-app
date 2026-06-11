package com.tih.app.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tih.app.dto.LanguageCreateRequest;
import com.tih.app.dto.LanguageDto;
import com.tih.app.exception.DuplicateResourceException;
import com.tih.app.exception.ResourceNotFoundException;
import com.tih.app.mapper.LanguageMapper;
import com.tih.app.model.Language;
import com.tih.app.repository.LanguageRepository;

@ExtendWith(MockitoExtension.class)
class LanguageServiceTest {

    private static final long LANGUAGE_ID = 1L;
    private static final long NON_EXISTENT_ID = 99L;
    private static final String JAVA_NAME = "Java";
    private static final String JAVA_CODE = "java";
    private static final String GO_NAME = "Go";
    private static final String GO_CODE = "go";
    private static final String GENERAL_NAME = "General";
    private static final String GENERAL_CODE = "general";

    @Mock
    private LanguageRepository languageRepository;
    @Mock
    private LanguageMapper languageMapper;

    @InjectMocks
    private LanguageService service;

    @Test
    void shouldReturnAllLanguages_whenRepositoryHasEntries() {
        // given
        Language java = buildLanguage(LANGUAGE_ID, JAVA_NAME, JAVA_CODE);
        Language general = buildLanguage(2L, GENERAL_NAME, GENERAL_CODE);
        LanguageDto javaDto = buildLanguageDto(LANGUAGE_ID, JAVA_NAME, JAVA_CODE);
        LanguageDto generalDto = buildLanguageDto(2L, GENERAL_NAME, GENERAL_CODE);

        when(languageRepository.findAll()).thenReturn(List.of(java, general));
        when(languageMapper.toDtoList(List.of(java, general))).thenReturn(List.of(javaDto, generalDto));

        // when
        List<LanguageDto> result = service.findAll();

        // then
        assertThat(result).hasSize(2)
                .extracting(LanguageDto::getCode)
                .containsExactly(JAVA_CODE, GENERAL_CODE);
    }

    @Test
    void shouldReturnEmptyList_whenNoLanguagesExist() {
        // given
        when(languageRepository.findAll()).thenReturn(List.of());
        when(languageMapper.toDtoList(List.of())).thenReturn(List.of());

        // when
        List<LanguageDto> result = service.findAll();

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnLanguageDto_whenLanguageExists() {
        // given
        Language language = buildLanguage(LANGUAGE_ID, JAVA_NAME, JAVA_CODE);
        LanguageDto dto = buildLanguageDto(LANGUAGE_ID, JAVA_NAME, JAVA_CODE);

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(languageMapper.toDto(language)).thenReturn(dto);

        // when
        LanguageDto result = service.findById(LANGUAGE_ID);

        // then
        assertThat(result.getId()).isEqualTo(LANGUAGE_ID);
        assertThat(result.getCode()).isEqualTo(JAVA_CODE);
    }

    @Test
    void shouldThrowResourceNotFoundException_whenLanguageNotFound() {
        // given
        when(languageRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.findById(NON_EXISTENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTENT_ID));
    }

    @Test
    void shouldCreateLanguage_whenCodeAndNameAreUnique() {
        // given
        LanguageCreateRequest request = buildRequest(GO_NAME, GO_CODE);
        Language entity = buildLanguage(3L, GO_NAME, GO_CODE);
        LanguageDto dto = buildLanguageDto(3L, GO_NAME, GO_CODE);

        when(languageRepository.existsByCode(GO_CODE)).thenReturn(false);
        when(languageRepository.existsByName(GO_NAME)).thenReturn(false);
        when(languageMapper.toEntity(request)).thenReturn(entity);
        when(languageRepository.save(entity)).thenReturn(entity);
        when(languageMapper.toDto(entity)).thenReturn(dto);

        // when
        LanguageDto result = service.create(request);

        // then
        assertThat(result.getCode()).isEqualTo(GO_CODE);
        verify(languageRepository).save(entity);
    }

    @Test
    void shouldThrowDuplicateResourceException_whenCodeAlreadyExists() {
        // given
        LanguageCreateRequest request = buildRequest(JAVA_NAME, JAVA_CODE);

        when(languageRepository.existsByCode(JAVA_CODE)).thenReturn(true);

        // when - then
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(JAVA_CODE);
        verify(languageRepository, never()).save(any());
    }

    @Test
    void shouldThrowDuplicateResourceException_whenNameAlreadyExists() {
        // given
        LanguageCreateRequest request = buildRequest(JAVA_NAME, "java-new");

        when(languageRepository.existsByCode("java-new")).thenReturn(false);
        when(languageRepository.existsByName(JAVA_NAME)).thenReturn(true);

        // when - then
        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(JAVA_NAME);
        verify(languageRepository, never()).save(any());
    }

    @Test
    void shouldUpdateLanguage_whenLanguageExists() {
        // given
        Language language = buildLanguage(LANGUAGE_ID, JAVA_NAME, JAVA_CODE);
        LanguageCreateRequest request = buildRequest("Java Updated", "java-updated");
        LanguageDto dto = buildLanguageDto(LANGUAGE_ID, "Java Updated", "java-updated");

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(languageRepository.save(language)).thenReturn(language);
        when(languageMapper.toDto(language)).thenReturn(dto);

        // when
        LanguageDto result = service.update(LANGUAGE_ID, request);

        // then
        assertThat(result.getName()).isEqualTo("Java Updated");
        verify(languageMapper).updateEntity(request, language);
        verify(languageRepository).save(language);
    }

    @Test
    void shouldThrowResourceNotFoundException_whenUpdatingNonExistentLanguage() {
        // given
        when(languageRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.update(NON_EXISTENT_ID, buildRequest("X", "x")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTENT_ID));
        verify(languageRepository, never()).save(any());
    }

    @Test
    void shouldDeleteLanguage_whenLanguageExists() {
        // given
        Language language = buildLanguage(LANGUAGE_ID, JAVA_NAME, JAVA_CODE);

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));

        // when
        service.delete(LANGUAGE_ID);

        // then
        verify(languageRepository).deleteById(LANGUAGE_ID);
    }

    @Test
    void shouldThrowResourceNotFoundException_whenDeletingNonExistentLanguage() {
        // given
        when(languageRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.delete(NON_EXISTENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTENT_ID));
        verify(languageRepository, never()).deleteById(any());
    }

    private Language buildLanguage(Long id, String name, String code) {
        return Language.builder()
                .id(id)
                .name(name)
                .code(code)
                .build();
    }

    private LanguageDto buildLanguageDto(Long id, String name, String code) {
        return LanguageDto.builder()
                .id(id)
                .name(name)
                .code(code)
                .build();
    }

    private LanguageCreateRequest buildRequest(String name, String code) {
        return new LanguageCreateRequest(name, code);
    }
}
