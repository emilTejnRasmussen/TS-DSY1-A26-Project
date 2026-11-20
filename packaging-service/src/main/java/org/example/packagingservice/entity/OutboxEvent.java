package org.example.packagingservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "outbox_event")
public class OutboxEvent
{
    @Id
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String eventType;

    @Column(nullable = false, updatable = false)
    private String routingKey;

    @Column(nullable = false, updatable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private boolean published = false;

    private Instant publishedAt;

    public OutboxEvent(
            UUID id,
            String eventType,
            String routingKey,
            String payload,
            Instant occurredAt)
    {
        this.id = id;
        this.eventType = eventType;
        this.routingKey = routingKey;
        this.payload = payload;
        this.occurredAt = occurredAt;
    }

    public void markPublished()
    {
        this.published = true;
        this.publishedAt = Instant.now();
    }
}
