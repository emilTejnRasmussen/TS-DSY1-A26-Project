package org.example.packagingservice.messaging;

import org.example.packagingservice.entity.OutboxEvent;
import org.example.packagingservice.repository.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherTest
{
    @Mock
    private OutboxEventRepository outboxRepo;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private OutboxPublisher publisher;
    private OutboxEvent event;

    @BeforeEach
    void setUp()
    {
        publisher = new OutboxPublisher(outboxRepo, rabbitTemplate);
        UUID eventId = UUID.randomUUID();
        event = new OutboxEvent(eventId, "ProductCreated", RabbitMqConfig.PRODUCT_CREATED_KEY,
                "{\"eventId\":\"" + eventId + "\",\"productId\":1}", Instant.now());
        when(outboxRepo.findByPublishedFalseOrderByOccurredAtAsc()).thenReturn(List.of(event));
    }

    @Test
    void marksPublishedOnlyAfterPositiveConfirmation()
    {
        doAnswer(invocation -> {
            assertFalse(event.isPublished());
            verify(outboxRepo, never()).save(any());
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        publisher.publishPendingEvents();

        assertTrue(event.isPublished());
        assertNotNull(event.getPublishedAt());
        verify(outboxRepo).save(event);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate).send(eq(RabbitMqConfig.EVENTS_EXCHANGE),
                eq(RabbitMqConfig.PRODUCT_CREATED_KEY), messageCaptor.capture(), any(CorrelationData.class));
        Message message = messageCaptor.getValue();
        assertEquals("application/json", message.getMessageProperties().getContentType());
        assertEquals(MessageDeliveryMode.PERSISTENT, message.getMessageProperties().getDeliveryMode());
        assertEquals(event.getId().toString(), message.getMessageProperties().getMessageId());
        assertEquals(event.getPayload(), new String(message.getBody(), StandardCharsets.UTF_8));
    }

    @Test
    void leavesRejectedEventUnpublished()
    {
        confirm(false, "Rejected");

        publisher.publishPendingEvents();

        assertUnpublished();
    }

    @Test
    void leavesReturnedEventUnpublishedEvenWhenConfirmed()
    {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.setReturned(new ReturnedMessage(invocation.getArgument(2), 312, "NO_ROUTE",
                    RabbitMqConfig.EVENTS_EXCHANGE, RabbitMqConfig.PRODUCT_CREATED_KEY));
            correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        publisher.publishPendingEvents();

        assertUnpublished();
    }

    @Test
    void leavesEventUnpublishedWhenBrokerIsUnavailable()
    {
        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        assertDoesNotThrow(publisher::publishPendingEvents);

        assertUnpublished();
    }

    @Test
    void leavesEventUnpublishedWhenConfirmationFails()
    {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().completeExceptionally(new TimeoutException("Confirmation unavailable"));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        publisher.publishPendingEvents();

        assertUnpublished();
    }

    @Test
    void retriesWithOriginalMessageIdAndPayload()
    {
        doThrow(new AmqpConnectException(new ConnectException("Connection refused")))
                .doAnswer(invocation -> {
                    CorrelationData correlation = invocation.getArgument(3);
                    correlation.getFuture().complete(new CorrelationData.Confirm(true, null));
                    return null;
                }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));

        publisher.publishPendingEvents();
        assertUnpublished();
        publisher.publishPendingEvents();

        assertTrue(event.isPublished());
        ArgumentCaptor<Message> messages = ArgumentCaptor.forClass(Message.class);
        verify(rabbitTemplate, times(2)).send(eq(RabbitMqConfig.EVENTS_EXCHANGE),
                eq(RabbitMqConfig.PRODUCT_CREATED_KEY), messages.capture(), any(CorrelationData.class));
        for (Message message : messages.getAllValues())
        {
            assertEquals(event.getId().toString(), message.getMessageProperties().getMessageId());
            assertArrayEquals(event.getPayload().getBytes(StandardCharsets.UTF_8), message.getBody());
        }
        verify(outboxRepo).save(event);
    }

    private void confirm(boolean ack, String reason)
    {
        doAnswer(invocation -> {
            CorrelationData correlation = invocation.getArgument(3);
            correlation.getFuture().complete(new CorrelationData.Confirm(ack, reason));
            return null;
        }).when(rabbitTemplate).send(anyString(), anyString(), any(Message.class), any(CorrelationData.class));
    }

    private void assertUnpublished()
    {
        assertFalse(event.isPublished());
        assertNull(event.getPublishedAt());
        verify(outboxRepo, never()).save(any());
    }
}
