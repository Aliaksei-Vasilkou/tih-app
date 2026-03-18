package com.tih.app.mapper;

import com.tih.app.dto.QuestionCreateRequest;
import com.tih.app.dto.QuestionDto;
import com.tih.app.model.Question;
import com.tih.app.model.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.Collections;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface QuestionMapper {

    @Mapping(source = "language.id", target = "languageId")
    @Mapping(source = "language.name", target = "languageName")
    @Mapping(source = "language.code", target = "languageCode")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "category.name", target = "categoryName")
    @Mapping(source = "tags", target = "tags", qualifiedByName = "tagsToNames")
    QuestionDto toDto(Question question);

    List<QuestionDto> toDtoList(List<Question> questions);

    @Mapping(target = "language", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "searchVector", ignore = true)
    Question toEntity(QuestionCreateRequest request);

    @Mapping(target = "language", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "tags", ignore = true)
    @Mapping(target = "searchVector", ignore = true)
    void updateEntity(QuestionCreateRequest request, @MappingTarget Question question);

    @Named("tagsToNames")
    default List<String> tagsToNames(List<Tag> tags) {
        if (tags == null) return Collections.emptyList();
        return tags.stream().map(Tag::getName).sorted().toList();
    }
}
