package org.example.animalprocessingservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.example.animalprocessingservice.entity.PartType;

import java.math.BigDecimal;

public record RegisterPartDto(
        @NotBlank
        String animalRegistrationNumber,

        @NotNull
        PartType type,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal weight
) { }