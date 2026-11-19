package org.example.traceabilityservice.messaging;

import org.example.traceabilityservice.entity.FailedEvent;
import org.example.traceabilityservice.entity.PartType;
import org.example.traceabilityservice.repository.FailedEventRepository;
import org.example.traceabilityservice.service.EventProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraceabilityEventConsumerTest
{
    @Mock private EventProcessingService events;
    @Mock private FailedEventRepository failedRepo;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private TraceabilityEventConsumer consumer;

    @BeforeEach
    void setUp()
    {
        consumer = new TraceabilityEventConsumer(events, failedRepo, objectMapper);
    }

    @Test
    void parsesAndDispatchesAnimalRegisteredContract()
    {
        AnimalRegisteredEvent event = animal();

        consumer.consumeAnimalRegistered(objectMapper.writeValueAsString(event));

        verify(events).registerAnimal(event);
        verifyNoInteractions(failedRepo);
    }

    @Test
    void parsesAndDispatchesPartRegisteredContract()
    {
        PartRegisteredEvent event = new PartRegisteredEvent(UUID.randomUUID(), "PartRegistered",
                Instant.now(), 101L, "ANIMAL-A", PartType.LEG, new BigDecimal("1.25"));

        consumer.consumePartRegistered(objectMapper.writeValueAsString(event));

        verify(events).registerPart(event);
        verifyNoInteractions(failedRepo);
    }

    @Test
    void parsesAndDispatchesTraySealedContract()
    {
        TraySealedEvent event = new TraySealedEvent(UUID.randomUUID(), "TraySealed", Instant.now(),
                10L, PartType.LEG, new BigDecimal("1.25"), List.of(
                new TraySealedEvent.TrayPart(101L, "ANIMAL-A", new BigDecimal("1.25"))));

        consumer.consumeTraySealed(objectMapper.writeValueAsString(event));

        verify(events).sealTray(event);
        verifyNoInteractions(failedRepo);
    }

    @Test
    void parsesAndDispatchesProductCreatedContract()
    {
        ProductCreatedEvent event = new ProductCreatedEvent(UUID.randomUUID(), "ProductCreated", Instant.now(),
                1L, List.of(new ProductCreatedEvent.ProductPart(101L, 10L, "ANIMAL-A", new BigDecimal("1.25"))));

        consumer.consumeProductCreated(objectMapper.writeValueAsString(event));

        verify(events).createProduct(event);
        verifyNoInteractions(failedRepo);
    }

    @Test
    void retainsMalformedPayloadInsteadOfDiscardingOrRetryingItForever()
    {
        String malformed = "{not JSON";

        assertDoesNotThrow(() -> consumer.consumeProductCreated(malformed));

        ArgumentCaptor<FailedEvent> captured = ArgumentCaptor.forClass(FailedEvent.class);
        verify(failedRepo).save(captured.capture());
        assertEquals(malformed, captured.getValue().getPayload());
        assertNotNull(captured.getValue().getReason());
        verifyNoInteractions(events);
    }

    @Test
    void retainsConflictingEventAfterTransactionalProcessorRejectsIt()
    {
        AnimalRegisteredEvent event = animal();
        String payload = objectMapper.writeValueAsString(event);
        doThrow(new IllegalArgumentException("Conflicting animal reference"))
                .when(events).registerAnimal(any());

        assertDoesNotThrow(() -> consumer.consumeAnimalRegistered(payload));

        ArgumentCaptor<FailedEvent> captured = ArgumentCaptor.forClass(FailedEvent.class);
        verify(failedRepo).save(captured.capture());
        assertEquals(payload, captured.getValue().getPayload());
        assertTrue(captured.getValue().getReason().contains("Conflicting animal reference"));
    }

    @Test
    void databaseFailurePropagatesSoRabbitCanRetry()
    {
        doThrow(new DataAccessResourceFailureException("Database unavailable"))
                .when(events).registerAnimal(any());

        assertThrows(DataAccessResourceFailureException.class,
                () -> consumer.consumeAnimalRegistered(objectMapper.writeValueAsString(animal())));

        verifyNoInteractions(failedRepo);
    }

    @Test
    void failureToRetainInvalidPayloadPropagatesRatherThanAcknowledgingIt()
    {
        when(failedRepo.save(any())).thenThrow(new DataAccessResourceFailureException("Database unavailable"));

        assertThrows(DataAccessResourceFailureException.class,
                () -> consumer.consumeProductCreated("{not JSON"));

        verifyNoInteractions(events);
    }

    @Test
    void identicalMalformedMessagesHaveTheSameFailureIdentifier()
    {
        consumer.consumeProductCreated("{not JSON");
        consumer.consumeProductCreated("{not JSON");

        ArgumentCaptor<FailedEvent> captured = ArgumentCaptor.forClass(FailedEvent.class);
        verify(failedRepo, times(2)).save(captured.capture());
        assertEquals(captured.getAllValues().getFirst().getId(), captured.getAllValues().getLast().getId());
    }

    private AnimalRegisteredEvent animal()
    {
        return new AnimalRegisteredEvent(UUID.randomUUID(), "AnimalRegistered", Instant.now(), "ANIMAL-A");
    }
}
