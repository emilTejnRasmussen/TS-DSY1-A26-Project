package events;

import java.time.Instant;
import java.util.UUID;

public record AnimalRegisteredEvent(
        UUID eventId,
        String eventType,
        Instant occurredAt,
        String registrationNumber
) {}