package org.example.animalprocessingservice.messaging;

import lombok.RequiredArgsConstructor;
import org.example.animalprocessingservice.entity.OutboxEvent;
import org.example.animalprocessingservice.entity.Part;
import org.example.animalprocessingservice.entity.Tray;
import org.example.animalprocessingservice.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY) // It means that the method must be called within an existing transaction
public class OutboxService
{
    private final OutboxEventRepository outboxRepo;
    private final ObjectMapper objectMapper;

    public void recordPartRegistered(Part part)
    {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        PartRegisteredEvent event = new PartRegisteredEvent(
                eventId,
                "PartRegistered",
                occurredAt,
                part.getId(),
                part.getAnimalReference().getRegistrationNumber(),
                part.getType(),
                part.getWeight()
        );

        saveEvent(
                event.eventId(),
                event.eventType(),
                RabbitMqConfig.PART_REGISTERED_KEY,
                event,
                event.occurredAt()
        );
    }

    public void recordTraySealed(Tray tray)
    {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        List<Part> parts = tray.getParts();
        List<TraySealedEvent.TrayPart> trayParts = new ArrayList<>();

        for (Part part : parts){
            trayParts.add(new TraySealedEvent.TrayPart(
                    part.getId(),
                    part.getAnimalReference().getRegistrationNumber(),
                    part.getWeight()
            ));
        }

        TraySealedEvent event = new TraySealedEvent(
                eventId,
                "TraySealed",
                occurredAt,
                tray.getId(),
                tray.getType(),
                tray.getCurrentWeight(),
                trayParts
        );

        saveEvent(
                event.eventId(),
                event.eventType(),
                RabbitMqConfig.TRAY_SEALED_KEY,
                event,
                event.occurredAt()
        );
    }

    private void saveEvent(
            UUID eventId,
            String eventType,
            String routingKey,
            Object event,
            Instant occurredAt)
    {
        String payload = objectMapper.writeValueAsString(event);

        OutboxEvent outboxEvent = new OutboxEvent(
                eventId,
                eventType,
                routingKey,
                payload,
                occurredAt
        );

        outboxRepo.save(outboxEvent);
    }
}