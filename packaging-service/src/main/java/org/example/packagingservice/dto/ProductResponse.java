package org.example.packagingservice.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductResponse(
        Long id,
        String name,
        BigDecimal totalWeight,
        List<PartResponse> parts,
        LocalDateTime createdAt
)
{
}
