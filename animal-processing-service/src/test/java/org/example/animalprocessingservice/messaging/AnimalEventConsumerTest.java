package org.example.animalprocessingservice.messaging;

import org.example.animalprocessingservice.entity.AnimalReference;
import org.example.animalprocessingservice.repository.AnimalReferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnimalEventConsumerTest
{
    @Mock
    private AnimalReferenceRepository animalReferenceRepo;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private AnimalEventConsumer consumer;

    @BeforeEach
    void setUp()
    {
        consumer = new AnimalEventConsumer(animalReferenceRepo, objectMapper);
    }

    @Test
    void savesAnimalReferenceFromTheIncomingEvent()
    {
        AnimalRegisteredEvent event = event();

        consumer.consume(objectMapper.writeValueAsString(event));

        ArgumentCaptor<AnimalReference> captured = ArgumentCaptor.forClass(AnimalReference.class);
        verify(animalReferenceRepo).save(captured.capture());
        assertEquals(event.registrationNumber(), captured.getValue().getRegistrationNumber());
    }

    @Test
    void duplicateDeliveryDoesNotSaveAnotherAnimalReference()
    {
        when(animalReferenceRepo.existsById("ANIMAL-A")).thenReturn(false, true);
        String payload = objectMapper.writeValueAsString(event());

        consumer.consume(payload);
        consumer.consume(payload);

        verify(animalReferenceRepo, times(1)).save(any(AnimalReference.class));
    }

    @Test
    void malformedJsonDoesNotPersistAnAnimalReference()
    {
        assertThrows(JacksonException.class, () -> consumer.consume("{not JSON"));

        verifyNoInteractions(animalReferenceRepo);
    }

    private AnimalRegisteredEvent event()
    {
        return new AnimalRegisteredEvent(UUID.randomUUID(), "AnimalRegistered", Instant.now(), "ANIMAL-A");
    }
}
