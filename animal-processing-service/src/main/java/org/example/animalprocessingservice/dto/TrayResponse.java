package org.example.animalprocessingservice.dto;

import org.example.animalprocessingservice.entity.PartType;
import org.example.animalprocessingservice.entity.TrayStatus;

import java.math.BigDecimal;
import java.util.List;

public record TrayResponse(
        Long id,
        PartType type,
        TrayStatus status,
        BigDecimal maxWeight,
        BigDecimal currentWeight,
        List<Long> partIds
){}
