package com.renthub.booking.mapper;

import com.renthub.booking.model.dto.BookingDto;
import com.renthub.booking.model.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    @Mapping(target = "equipmentId", source = "equipment.id")
    @Mapping(target = "equipmentTitle", source = "equipment.title")
    @Mapping(target = "equipmentImageUrl", source = "equipment.imageUrl")
    @Mapping(target = "ownerId", source = "equipment.owner.id")
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", expression = "java(booking.getCustomer() != null ? (booking.getCustomer().getFirstName() + \" \" + booking.getCustomer().getLastName()).trim() : null)")
    BookingDto toDto(Booking booking);
}
