package org.example.animalregistrationservice.dto.animal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.example.animalregistrationservice.enitity.AnimalType;

import java.math.BigDecimal;

public record RegisterAnimalDto(
        @NotNull
        AnimalType type,

        @NotNull
        @Positive
        BigDecimal weight,

        @NotNull
        @Positive
        Long farmId
){}
