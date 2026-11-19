package org.example.packagingservice.dto;

import org.example.packagingservice.entity.PartType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

public record TrayResponse(
        Long id,
        PartType type,
        BigDecimal totalWeight,
        BigDecimal availableWeight,
        List<PartResponse> parts,
        Instant sealedAt,
        LocalDateTime receivedAt
)
{
}
