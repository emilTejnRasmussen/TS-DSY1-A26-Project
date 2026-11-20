package org.example.traceabilityservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
public class FailedEvent
{
    @Id
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String eventType;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime failedAt;

    public FailedEvent(String eventType, String payload, String reason)
    {
        this.id = UUID.nameUUIDFromBytes((eventType + "\n" + payload).getBytes(StandardCharsets.UTF_8));
        this.eventType = eventType;
        this.payload = payload;
        this.reason = reason;
        this.failedAt = LocalDateTime.now();
    }
}
