package org.example.animalprocessingservice.dto;

import org.example.animalprocessingservice.entity.PartType;

import java.math.BigDecimal;

public record PartDto(
        Long id,
        BigDecimal weight,
        PartType type,
        String animalRegistrationNumber,
        Long trayId
)
{
}
