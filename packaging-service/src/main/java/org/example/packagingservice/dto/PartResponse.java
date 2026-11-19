package org.example.packagingservice.dto;

import org.example.packagingservice.entity.PartType;

import java.math.BigDecimal;

public record PartResponse(
        Long id,
        PartType type,
        BigDecimal weight,
        String animalRegistrationNumber,
        Long trayId,
        Long productId
)
{
}
