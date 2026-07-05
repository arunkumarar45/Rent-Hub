package com.renthub.equipment.model.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentDto {
    private Long id;
    private Long ownerId;
    private String ownerName;
    private Long categoryId;
    private String categoryName;
    private String categorySlug;
    private String title;
    private String description;
    private Integer dailyRate; // in cents
    private Integer deposit; // in cents
    private String condition;
    private String location;
    private String imageUrl;
    private Boolean isActive;
    private List<String> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
