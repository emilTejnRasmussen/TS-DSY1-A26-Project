package org.example.animalregistrationservice.dto.animal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record RegisterAnimalDto(
        @NotNull
        @Positive
        BigDecimal weight,

        @NotNull
        @Positive
        Long farmId
){}
