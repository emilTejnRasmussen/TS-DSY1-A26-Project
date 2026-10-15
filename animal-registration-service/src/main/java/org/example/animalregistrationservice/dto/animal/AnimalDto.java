package org.example.animalregistrationservice.dto.animal;

import org.example.animalregistrationservice.enitity.AnimalType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AnimalDto(
        String registrationNumber,
        AnimalType type,
        LocalDateTime registeredAt,
        BigDecimal weight,
        Long farmId,
        String farmName
) {}