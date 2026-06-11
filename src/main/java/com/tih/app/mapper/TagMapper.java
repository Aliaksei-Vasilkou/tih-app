package com.tih.app.mapper;

import com.tih.app.dto.TagDto;
import com.tih.app.model.Tag;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TagMapper {

    @Mapping(source = "language.id", target = "languageId")
    @Mapping(source = "language.name", target = "languageName")
    @Mapping(source = "language.code", target = "languageCode")
    TagDto toDto(Tag tag);

    List<TagDto> toDtoList(List<Tag> tags);
}
