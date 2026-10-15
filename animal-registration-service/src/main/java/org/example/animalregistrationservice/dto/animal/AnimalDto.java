package org.example.animalregistrationservice.dto.animal;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AnimalDto(
        String registrationNumber,
        LocalDate arrivalDate,
        BigDecimal weight,
        Long farmId,
        String farmName
) {}