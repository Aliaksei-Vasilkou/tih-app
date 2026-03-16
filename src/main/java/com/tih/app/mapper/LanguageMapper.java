package com.tih.app.mapper;

import com.tih.app.dto.LanguageCreateRequest;
import com.tih.app.dto.LanguageDto;
import com.tih.app.model.Language;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LanguageMapper {

    LanguageDto toDto(Language language);

    List<LanguageDto> toDtoList(List<Language> languages);

    Language toEntity(LanguageCreateRequest request);

    void updateEntity(LanguageCreateRequest request, @MappingTarget Language language);
}
