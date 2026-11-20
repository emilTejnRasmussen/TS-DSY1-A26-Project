package org.example.animalprocessingservice.messaging;

import org.example.animalprocessingservice.entity.PartType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PartRegisteredEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        Long partId,
        String animalRegistrationNumber,
        PartType partType,
        BigDecimal weight
) {
}