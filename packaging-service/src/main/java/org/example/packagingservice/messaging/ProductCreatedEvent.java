package org.example.packagingservice.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductCreatedEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        Long productId,
        List<ProductPart> parts
)
{
    public record ProductPart(
            Long partId,
            Long trayId,
            String animalRegistrationNumber,
            BigDecimal weight
    ) {}
}
