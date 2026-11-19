package org.example.traceabilityservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.traceabilityservice.entity.FailedEvent;
import org.example.traceabilityservice.repository.FailedEventRepository;
import org.example.traceabilityservice.service.EventProcessingService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class TraceabilityEventConsumer
{
    private final EventProcessingService eventProcessingService;
    private final FailedEventRepository failedEventRepo;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMqConfig.ANIMAL_REGISTERED_QUEUE)
    public void consumeAnimalRegistered(String message)
    {
        consume(message, "AnimalRegistered");
    }

    @RabbitListener(queues = RabbitMqConfig.PART_REGISTERED_QUEUE)
    public void consumePartRegistered(String message)
    {
        consume(message, "PartRegistered");
    }

    @RabbitListener(queues = RabbitMqConfig.TRAY_SEALED_QUEUE)
    public void consumeTraySealed(String message)
    {
        consume(message, "TraySealed");
    }

    @RabbitListener(queues = RabbitMqConfig.PRODUCT_CREATED_QUEUE)
    public void consumeProductCreated(String message)
    {
        consume(message, "ProductCreated");
    }

    private void consume(String message, String eventType)
    {
        try
        {
            switch (eventType)
            {
                case "AnimalRegistered" -> eventProcessingService.registerAnimal(
                        objectMapper.readValue(message, AnimalRegisteredEvent.class));
                case "PartRegistered" -> eventProcessingService.registerPart(
                        objectMapper.readValue(message, PartRegisteredEvent.class));
                case "TraySealed" -> eventProcessingService.sealTray(
                        objectMapper.readValue(message, TraySealedEvent.class));
                case "ProductCreated" -> eventProcessingService.createProduct(
                        objectMapper.readValue(message, ProductCreatedEvent.class));
                default -> throw new IllegalArgumentException("Unsupported event type");
            }
        } catch (JacksonException | IllegalArgumentException e)
        {
            // The projection transaction has rolled back before retaining the failed delivery.
            FailedEvent failure = new FailedEvent(eventType, message,
                    e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
            failedEventRepo.save(failure);
            log.error("Retained invalid {} delivery in failed_event: {}", eventType, failure.getId(), e);
        }
    }
}
