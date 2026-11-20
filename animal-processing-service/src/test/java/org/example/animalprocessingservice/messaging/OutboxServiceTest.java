package org.example.animalprocessingservice.messaging;

import org.example.animalprocessingservice.entity.AnimalReference;
import org.example.animalprocessingservice.entity.OutboxEvent;
import org.example.animalprocessingservice.entity.Part;
import org.example.animalprocessingservice.entity.PartType;
import org.example.animalprocessingservice.entity.Tray;
import org.example.animalprocessingservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest
{
    @Mock
    private OutboxEventRepository outboxRepo;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private OutboxService outboxService;

    @BeforeEach
    void setUp()
    {
        outboxService = new OutboxService(outboxRepo, objectMapper);
    }

    @Test
    void recordsPartRegistrationWithItsOriginalAnimalAndWeight()
    {
        Part part = part(101L, "ANIMAL-A", "1.25");

        outboxService.recordPartRegistered(part);

        OutboxEvent stored = savedEvent();
        PartRegisteredEvent payload = objectMapper.readValue(stored.getPayload(), PartRegisteredEvent.class);
        assertEquals("part.registered", stored.getRoutingKey());
        assertEquals("PartRegistered", stored.getEventType());
        assertEquals(stored.getEventType(), payload.eventType());
        assertNotNull(stored.getId());
        assertEquals(stored.getId(), payload.eventId());
        assertEquals(stored.getOccurredAt(), payload.occurredAt());
        assertEquals(101L, payload.partId());
        assertEquals("ANIMAL-A", payload.animalRegistrationNumber());
        assertEquals(PartType.LEG, payload.partType());
        assertEquals(new BigDecimal("1.25"), payload.weight());
        assertFalse(stored.isPublished());
        assertNull(stored.getPublishedAt());
    }

    @Test
    void recordsSealedTrayWithEveryPartAndExactTotalWeight()
    {
        Tray tray = new Tray();
        tray.setId(10L);
        tray.setType(PartType.LEG);
        tray.getParts().addAll(List.of(
                part(101L, "ANIMAL-A", "0.10"),
                part(102L, "ANIMAL-B", "0.20")
        ));

        outboxService.recordTraySealed(tray);

        OutboxEvent stored = savedEvent();
        TraySealedEvent payload = objectMapper.readValue(stored.getPayload(), TraySealedEvent.class);
        assertEquals("tray.sealed", stored.getRoutingKey());
        assertEquals("TraySealed", stored.getEventType());
        assertEquals(stored.getEventType(), payload.eventType());
        assertNotNull(stored.getId());
        assertEquals(stored.getId(), payload.eventId());
        assertEquals(stored.getOccurredAt(), payload.occurredAt());
        assertEquals(10L, payload.trayId());
        assertEquals(PartType.LEG, payload.partType());
        assertEquals(new BigDecimal("0.30"), payload.totalWeight());
        assertEquals(List.of(
                new TraySealedEvent.TrayPart(101L, "ANIMAL-A", new BigDecimal("0.10")),
                new TraySealedEvent.TrayPart(102L, "ANIMAL-B", new BigDecimal("0.20"))
        ), payload.parts());
        assertFalse(stored.isPublished());
        assertNull(stored.getPublishedAt());
    }

    private OutboxEvent savedEvent()
    {
        ArgumentCaptor<OutboxEvent> captured = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepo).save(captured.capture());
        return captured.getValue();
    }

    private Part part(long id, String registrationNumber, String weight)
    {
        AnimalReference animal = new AnimalReference();
        animal.setRegistrationNumber(registrationNumber);
        Part part = new Part();
        part.setId(id);
        part.setAnimalReference(animal);
        part.setType(PartType.LEG);
        part.setWeight(new BigDecimal(weight));
        return part;
    }
}
