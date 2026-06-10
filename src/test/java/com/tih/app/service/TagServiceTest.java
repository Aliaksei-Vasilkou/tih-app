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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagServiceTest {

    private static final long LANGUAGE_ID = 1L;
    private static final long OTHER_LANGUAGE_ID = 2L;
    private static final long TAG_ID = 10L;
    private static final long NON_EXISTENT_ID = 99L;
    private static final String CONCURRENCY = "Concurrency";

    @Mock
    private TagRepository tagRepository;
    @Mock
    private LanguageRepository languageRepository;
    @Mock
    private TagMapper tagMapper;

    @InjectMocks
    private TagService service;

    @Test
    void shouldReturnTags_whenLanguageExists() {
        // given
        Language language = buildLanguage(LANGUAGE_ID);
        Tag tag = buildTag(TAG_ID, "Garbage Collection", language);
        TagDto dto = TagDto.builder().id(TAG_ID).name("Garbage Collection").build();

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(tagRepository.findAllByLanguageId(LANGUAGE_ID)).thenReturn(List.of(tag));
        when(tagMapper.toDtoList(List.of(tag))).thenReturn(List.of(dto));

        // when
        List<TagDto> result = service.findAllByLanguageId(LANGUAGE_ID);

        // then
        assertThat(result).hasSize(1)
                .extracting(TagDto::getName)
                .containsExactly("Garbage Collection");
    }

    @Test
    void shouldThrowResourceNotFoundException_whenLanguageNotFoundOnFindAll() {
        // given
        when(languageRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.findAllByLanguageId(NON_EXISTENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTENT_ID));
        verify(tagRepository, never()).findAllByLanguageId(any());
    }

    @Test
    void shouldReturnEmptyList_whenNoTagsForLanguage() {
        // given
        Language language = buildLanguage(LANGUAGE_ID);

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(tagRepository.findAllByLanguageId(LANGUAGE_ID)).thenReturn(List.of());
        when(tagMapper.toDtoList(List.of())).thenReturn(List.of());

        // when
        List<TagDto> result = service.findAllByLanguageId(LANGUAGE_ID);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnTagDto_whenTagExists() {
        // given
        Language language = buildLanguage(LANGUAGE_ID);
        Tag tag = buildTag(TAG_ID, "ACID", language);
        TagDto dto = TagDto.builder().id(TAG_ID).name("ACID").build();

        when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag));
        when(tagMapper.toDto(tag)).thenReturn(dto);

        // when
        TagDto result = service.findById(TAG_ID);

        // then
        assertThat(result.getId()).isEqualTo(TAG_ID);
        assertThat(result.getName()).isEqualTo("ACID");
    }

    @Test
    void shouldThrowResourceNotFoundException_whenTagNotFound() {
        // given
        when(tagRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.findById(NON_EXISTENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTENT_ID));
    }

    @Test
    void shouldCreateTag_whenNameIsUniqueForLanguage() {
        // given
        Language language = buildLanguage(LANGUAGE_ID);
        TagCreateRequest request = TagCreateRequest.builder().name(CONCURRENCY).build();
        Tag saved = buildTag(20L, CONCURRENCY, language);
        TagDto dto = TagDto.builder().id(20L).name(CONCURRENCY).build();

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(tagRepository.existsByNameIgnoreCaseAndLanguageId(CONCURRENCY, LANGUAGE_ID)).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenReturn(saved);
        when(tagMapper.toDto(saved)).thenReturn(dto);

        // when
        TagDto result = service.create(LANGUAGE_ID, request);

        // then
        assertThat(result.getName()).isEqualTo(CONCURRENCY);
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    void shouldThrowResourceNotFoundException_whenLanguageNotFoundOnCreate() {
        // given
        when(languageRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.create(NON_EXISTENT_ID, TagCreateRequest.builder().name("X").build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTENT_ID));
        verify(tagRepository, never()).save(any());
    }

    @Test
    void shouldThrowDuplicateResourceException_whenTagNameExistsForLanguage() {
        // given
        Language language = buildLanguage(LANGUAGE_ID);

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(tagRepository.existsByNameIgnoreCaseAndLanguageId(CONCURRENCY, LANGUAGE_ID)).thenReturn(true);

        // when - then
        assertThatThrownBy(() -> service.create(LANGUAGE_ID, TagCreateRequest.builder().name(CONCURRENCY).build()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining(CONCURRENCY);
        verify(tagRepository, never()).save(any());
    }

    @Test
    void shouldTrimTagName_whenCreatingWithWhitespace() {
        // given — duplicate check uses the raw name; trimming only happens when building the entity
        Language language = buildLanguage(LANGUAGE_ID);
        TagCreateRequest request = TagCreateRequest.builder().name("  Streams  ").build();
        Tag saved = buildTag(21L, "Streams", language);
        TagDto dto = TagDto.builder().id(21L).name("Streams").build();

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(tagRepository.existsByNameIgnoreCaseAndLanguageId("  Streams  ", LANGUAGE_ID)).thenReturn(false);
        when(tagRepository.save(any(Tag.class))).thenReturn(saved);
        when(tagMapper.toDto(saved)).thenReturn(dto);

        // when
        TagDto result = service.create(LANGUAGE_ID, request);

        // then
        assertThat(result.getName()).isEqualTo("Streams");
    }

    @Test
    void shouldUpdateTagName_whenTagBelongsToLanguageAndNameIsUnique() {
        // given
        Language language = buildLanguage(LANGUAGE_ID);
        Tag tag = buildTag(TAG_ID, "OldName", language);
        TagCreateRequest request = TagCreateRequest.builder().name("NewName").build();
        Tag saved = buildTag(TAG_ID, "NewName", language);
        TagDto dto = TagDto.builder().id(TAG_ID).name("NewName").build();

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag));
        when(tagRepository.existsByNameIgnoreCaseAndLanguageId("NewName", LANGUAGE_ID)).thenReturn(false);
        when(tagRepository.save(tag)).thenReturn(saved);
        when(tagMapper.toDto(saved)).thenReturn(dto);

        // when
        TagDto result = service.update(LANGUAGE_ID, TAG_ID, request);

        // then
        assertThat(result.getName()).isEqualTo("NewName");
        verify(tagRepository).save(tag);
    }

    @Test
    void shouldThrowResourceNotFoundException_whenTagBelongsToDifferentLanguage() {
        // given
        Language language1 = buildLanguage(LANGUAGE_ID);
        Language language2 = buildLanguage(OTHER_LANGUAGE_ID);
        Tag tag = buildTag(TAG_ID, "SomeName", language2);

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language1));
        when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag));

        // when - then
        assertThatThrownBy(() -> service.update(LANGUAGE_ID, TAG_ID, TagCreateRequest.builder().name("NewName").build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(TAG_ID));
        verify(tagRepository, never()).save(any());
    }

    @Test
    void shouldThrowDuplicateResourceException_whenNewNameExistsForLanguageOnUpdate() {
        // given
        Language language = buildLanguage(LANGUAGE_ID);
        Tag tag = buildTag(TAG_ID, "OldName", language);
        TagCreateRequest request = TagCreateRequest.builder().name("ExistingName").build();

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag));
        when(tagRepository.existsByNameIgnoreCaseAndLanguageId("ExistingName", LANGUAGE_ID)).thenReturn(true);

        // when - then
        assertThatThrownBy(() -> service.update(LANGUAGE_ID, TAG_ID, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("ExistingName");
        verify(tagRepository, never()).save(any());
    }

    @Test
    void shouldUpdateWithoutDuplicateCheck_whenNameIsUnchanged() {
        // given — same name (case-insensitive), duplicate check must be skipped
        Language language = buildLanguage(LANGUAGE_ID);
        Tag tag = buildTag(TAG_ID, "SameName", language);
        TagCreateRequest request = TagCreateRequest.builder().name("samename").build();
        Tag saved = buildTag(TAG_ID, "samename", language);
        TagDto dto = TagDto.builder().id(TAG_ID).name("samename").build();

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag));
        when(tagRepository.save(tag)).thenReturn(saved);
        when(tagMapper.toDto(saved)).thenReturn(dto);

        // when
        TagDto result = service.update(LANGUAGE_ID, TAG_ID, request);

        // then
        assertThat(result.getName()).isEqualTo("samename");
        verify(tagRepository, never()).existsByNameIgnoreCaseAndLanguageId(any(), any());
    }

    @Test
    void shouldThrowResourceNotFoundException_whenLanguageNotFoundOnUpdate() {
        // given
        when(languageRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.update(NON_EXISTENT_ID, TAG_ID, TagCreateRequest.builder().name("X").build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTENT_ID));
        verify(tagRepository, never()).save(any());
    }

    @Test
    void shouldDeleteTag_whenTagBelongsToLanguage() {
        // given
        Language language = buildLanguage(LANGUAGE_ID);
        Tag tag = buildTag(TAG_ID, "GC", language);

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag));

        // when
        service.delete(LANGUAGE_ID, TAG_ID);

        // then
        verify(tagRepository).delete(tag);
    }

    @Test
    void shouldThrowResourceNotFoundException_whenDeletingTagBelongingToDifferentLanguage() {
        // given
        Language language1 = buildLanguage(LANGUAGE_ID);
        Language language2 = buildLanguage(OTHER_LANGUAGE_ID);
        Tag tag = buildTag(TAG_ID, "GC", language2);

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language1));
        when(tagRepository.findById(TAG_ID)).thenReturn(Optional.of(tag));

        // when - then
        assertThatThrownBy(() -> service.delete(LANGUAGE_ID, TAG_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(TAG_ID));
        verify(tagRepository, never()).delete(any());
    }

    @Test
    void shouldThrowResourceNotFoundException_whenTagNotFoundOnDelete() {
        // given
        Language language = buildLanguage(LANGUAGE_ID);

        when(languageRepository.findById(LANGUAGE_ID)).thenReturn(Optional.of(language));
        when(tagRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.delete(LANGUAGE_ID, NON_EXISTENT_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTENT_ID));
        verify(tagRepository, never()).delete(any());
    }

    @Test
    void shouldThrowResourceNotFoundException_whenLanguageNotFoundOnDelete() {
        // given
        when(languageRepository.findById(NON_EXISTENT_ID)).thenReturn(Optional.empty());

        // when - then
        assertThatThrownBy(() -> service.delete(NON_EXISTENT_ID, TAG_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(String.valueOf(NON_EXISTENT_ID));
        verify(tagRepository, never()).delete(any());
    }

    private Language buildLanguage(Long id) {
        return Language.builder()
                .id(id)
                .name("Lang" + id)
                .code("lang" + id)
                .build();
    }

    private Tag buildTag(Long id, String name, Language language) {
        return Tag.builder()
                .id(id)
                .name(name)
                .language(language)
                .build();
    }
}
