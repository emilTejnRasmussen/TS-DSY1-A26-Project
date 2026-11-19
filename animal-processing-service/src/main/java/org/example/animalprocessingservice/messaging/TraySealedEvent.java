package org.example.animalprocessingservice.messaging;

import org.example.animalprocessingservice.entity.PartType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TraySealedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        Long trayId,
        PartType partType,
        BigDecimal totalWeight,
        List<TrayPart> parts
)
{
    public record TrayPart(
            Long partId,
            String animalRegistrationNumber,
            BigDecimal weight
    ) {}
}