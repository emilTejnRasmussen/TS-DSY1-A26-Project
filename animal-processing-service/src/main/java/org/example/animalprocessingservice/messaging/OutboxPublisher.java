package org.example.animalprocessingservice.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.animalprocessingservice.entity.OutboxEvent;
import org.example.animalprocessingservice.repository.OutboxEventRepository;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher
{
    private final OutboxEventRepository outboxRepo;
    private final RabbitTemplate rabbitTemplate;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents()
    {
        List<OutboxEvent> events = outboxRepo.findByPublishedFalseOrderByOccurredAtAsc();

        for (OutboxEvent event : events)
        {
            if (Thread.currentThread().isInterrupted())
            {
                return;
            }

            publishEvent(event);
        }
    }

    private void publishEvent(OutboxEvent event)
    {
        try
        {
            Message message = MessageBuilder
                    .withBody(event.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setContentType("application/json")
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .setMessageId(event.getId().toString())
                    .build();

            CorrelationData correlationData =
                    new CorrelationData(UUID.randomUUID().toString());

            rabbitTemplate.send(
                    RabbitMqConfig.EVENTS_EXCHANGE,
                    event.getRoutingKey(),
                    message,
                    correlationData
            );

            CorrelationData.Confirm confirm = correlationData.getFuture().get(5, TimeUnit.SECONDS);

            if (!confirm.ack())
            {
                log.warn("RabbitMQ rejected event {}: {}", event.getId(), confirm.reason());
                return;
            }

            if (correlationData.getReturned() != null)
            {
                log.warn("Event {} was unroutable with routing key {}", event.getId(), event.getRoutingKey());
                return;
            }

            event.markPublished();
            outboxRepo.save(event);

            log.info("Published {} event: {}", event.getEventType(), event.getId());
        } catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();

            log.warn("Publishing interrupted for event {}",
                    event.getId(), e);
        } catch (Exception e)
        {
            log.error("Failed to publish event {}",
                    event.getId(), e);
        }
    }
}