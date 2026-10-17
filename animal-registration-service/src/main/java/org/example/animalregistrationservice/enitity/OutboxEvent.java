
package org.example.animalregistrationservice.enitity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Entity
@Table(name = "outbox_event")
public class OutboxEvent
{
    @Id
    private UUID id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private String registrationNumber;

    @Column(nullable = false)
    private Instant occurredAt;

    @Column(nullable = false)
    private boolean published = false;

    protected OutboxEvent()
    {
    }

    public OutboxEvent(String registrationNumber)
    {
        this.id = UUID.randomUUID();
        this.eventType = "AnimalRegistered";
        this.registrationNumber = registrationNumber;
        this.occurredAt = Instant.now();
    }

    public void markPublished()
    {
        this.published = true;
    }
}
