
package org.example.animalregistrationservice.messaging;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import org.example.animalregistrationservice.enitity.OutboxEvent;
import org.example.animalregistrationservice.repository.OutboxEventRepository;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class OutboxPublisher
{
    private static final String EXCHANGE = "slaughterhouse.events";
    private static final String ROUTING_KEY = "animal.registered";

    private final OutboxEventRepository repository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    public OutboxPublisher(
            OutboxEventRepository repository,
            RabbitTemplate rabbitTemplate,
            ObjectMapper objectMapper)
    {
        this.repository = repository;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents()
    {
        List<OutboxEvent> events = repository.findByPublishedFalse();

        for (OutboxEvent event : events)
        {
            try
            {
                String json = objectMapper.writeValueAsString(
                        new AnimalRegisteredEvent(
                                event.getId(),
                                event.getEventType(),
                                event.getOccurredAt(),
                                event.getRegistrationNumber()
                        )
                );

                CorrelationData correlation = new CorrelationData(event.getId().toString());

                rabbitTemplate.convertAndSend(
                        EXCHANGE,
                        ROUTING_KEY,
                        json,
                        message ->
                        {
                            message.getMessageProperties().setContentType("application/json");
                            message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                            return message;
                        },
                        correlation
                );

                var confirm = correlation.getFuture()
                        .get(5, TimeUnit.SECONDS);

                if (!confirm.ack())
                {
                    log.warn(
                            "RabbitMQ rejected event: eventId={}, reason={}",
                            event.getId(),
                            confirm.reason()
                    );

                    continue;
                }

                event.markPublished();
                repository.save(event);

                log.info(
                        "Published AnimalRegistered event: eventId={}, registrationNumber={}",
                        event.getId(),
                        event.getRegistrationNumber()
                );

            } catch (Exception e)
            {
                log.error("Failed to publish event: eventId={}", event.getId(), e);
                break;
            }
        }
    }
}
