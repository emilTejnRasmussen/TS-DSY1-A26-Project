package org.example.animalprocessingservice.dto;

import org.example.animalprocessingservice.entity.PartType;

import java.math.BigDecimal;

public record RegisterPartDto(
        String animalRegistrationNumber,
        PartType type,
        BigDecimal weight
) { }