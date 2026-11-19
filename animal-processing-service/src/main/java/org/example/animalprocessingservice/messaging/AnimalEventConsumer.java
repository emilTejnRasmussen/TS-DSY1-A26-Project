package org.example.animalprocessingservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.animalprocessingservice.entity.AnimalReference;
import org.example.animalprocessingservice.repository.AnimalReferenceRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnimalEventConsumer
{
    private final AnimalReferenceRepository animalReferenceRepo;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMqConfig.ANIMAL_REGISTERED_QUEUE)
    @Transactional
    public void consume(String message)
    {
        AnimalRegisteredEvent event = objectMapper.readValue(
                message,
                AnimalRegisteredEvent.class
        );

        if (animalReferenceRepo.existsById(event.registrationNumber()))
        {
            log.info("Animal reference already exists: {}",
                    event.registrationNumber());

            return;
        }

        AnimalReference animalReference = new AnimalReference();
        animalReference.setRegistrationNumber(event.registrationNumber());

        animalReferenceRepo.save(animalReference);

        log.info("Registered animal reference: {}",
                event.registrationNumber());
    }
}