package org.example.traceabilityservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
public class ProcessedEvent
{
    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime processedAt;

    public ProcessedEvent(UUID eventId)
    {
        this.eventId = eventId;
        this.processedAt = LocalDateTime.now();
    }
}
