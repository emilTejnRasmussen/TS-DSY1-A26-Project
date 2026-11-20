package org.example.animalprocessingservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import org.example.animalprocessingservice.entity.PartType;

import java.math.BigDecimal;

public record CreateTrayRequest(
        @NotNull
        PartType type,

        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 8, fraction = 2)
        BigDecimal maxWeight
)
{
}
