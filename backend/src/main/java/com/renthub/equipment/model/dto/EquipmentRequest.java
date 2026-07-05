package com.renthub.equipment.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentRequest {

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Daily rate is required")
    @Positive(message = "Daily rate must be positive")
    private Integer dailyRate; // in cents

    @NotNull(message = "Security deposit is required")
    @Positive(message = "Security deposit must be positive")
    private Integer deposit; // in cents

    @NotBlank(message = "Category slug is required")
    private String categorySlug;

    @NotBlank(message = "Condition is required")
    private String condition;

    @NotBlank(message = "Location is required")
    private String location;

    private String imageUrl;

    private List<String> images;
}
