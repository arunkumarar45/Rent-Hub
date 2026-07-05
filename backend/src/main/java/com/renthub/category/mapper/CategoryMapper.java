package com.renthub.category.mapper;

import com.renthub.category.model.dto.CategoryDto;
import com.renthub.category.model.entity.EquipmentCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * MapStruct mapper for EquipmentCategory ↔ CategoryDto.
 *
 * CategoryTreeDto is assembled manually in CategoryServiceImpl because
 * the self-referential children field would create a circular dependency
 * that MapStruct cannot resolve without additional complexity.
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    /**
     * Maps entity to flat DTO.
     * parent.id and parent.name are extracted to denormalized fields.
     * Null-safe: MapStruct generates null checks for nested paths.
     */
    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "parentName", source = "parent.name")
    CategoryDto toDto(EquipmentCategory category);

    /**
     * Maps a list of entities to a list of flat DTOs.
     */
    List<CategoryDto> toDtoList(List<EquipmentCategory> categories);
}
