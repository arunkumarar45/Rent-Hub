package com.renthub.equipment.mapper;

import com.renthub.equipment.model.dto.EquipmentDto;
import com.renthub.equipment.model.entity.Equipment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EquipmentMapper {

    @Mapping(target = "ownerId", source = "owner.id")
    @Mapping(target = "ownerName", expression = "java(equipment.getOwner() != null ? (equipment.getOwner().getFirstName() + \" \" + equipment.getOwner().getLastName()).trim() : null)")
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categorySlug", source = "category.slug")
    EquipmentDto toDto(Equipment equipment);
}
