package org.example.animalregistrationservice.dto.animal;

import org.example.animalregistrationservice.enitity.AnimalType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AnimalDto(
        String registrationNumber,
        AnimalType type,
        LocalDate arrivalDate,
        BigDecimal weight,
        Long farmId,
        String farmName
) {}