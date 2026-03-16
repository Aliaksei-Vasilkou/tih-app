package com.tih.app.mapper;

import com.tih.app.dto.CategoryCreateRequest;
import com.tih.app.dto.CategoryDto;
import com.tih.app.model.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    @Mapping(source = "language.id", target = "languageId")
    @Mapping(source = "language.name", target = "languageName")
    @Mapping(source = "language.code", target = "languageCode")
    CategoryDto toDto(Category category);

    List<CategoryDto> toDtoList(List<Category> categories);

    @Mapping(target = "language", ignore = true)
    Category toEntity(CategoryCreateRequest request);

    @Mapping(target = "language", ignore = true)
    void updateEntity(CategoryCreateRequest request, @MappingTarget Category category);
}
