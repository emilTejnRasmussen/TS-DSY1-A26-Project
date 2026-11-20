package org.example.packagingservice.messaging;

import lombok.RequiredArgsConstructor;
import org.example.packagingservice.entity.OutboxEvent;
import org.example.packagingservice.entity.Product;
import org.example.packagingservice.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class OutboxService
{
    private final OutboxEventRepository outboxRepo;
    private final ObjectMapper objectMapper;

    public void recordProductCreated(Product product)
    {
        ProductCreatedEvent event = new ProductCreatedEvent(
                UUID.randomUUID(),
                "ProductCreated",
                Instant.now(),
                product.getId(),
                product.getParts().stream()
                        .map(part -> new ProductCreatedEvent.ProductPart(
                                part.getId(),
                                part.getTray().getId(),
                                part.getAnimalRegistrationNumber(),
                                part.getWeight()
                        ))
                        .toList()
        );

        outboxRepo.save(new OutboxEvent(
                event.eventId(),
                event.eventType(),
                RabbitMqConfig.PRODUCT_CREATED_KEY,
                objectMapper.writeValueAsString(event),
                event.occurredAt()
        ));
    }
}
