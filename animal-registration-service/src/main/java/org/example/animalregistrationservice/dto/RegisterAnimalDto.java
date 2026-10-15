package org.example.animalregistrationservice.dto;

import java.math.BigDecimal;

public record RegisterAnimalDto(
        BigDecimal weight,
        Long farmId
){}
