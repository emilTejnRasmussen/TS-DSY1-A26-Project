package org.example.traceabilityservice;

import grpc.AnimalRequest;
import grpc.ProductRequest;
import grpc.TraceabilityServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.example.traceabilityservice.grpcservice.GrpcServerLifecycle;
import org.example.traceabilityservice.messaging.ProductCreatedEvent;
import org.example.traceabilityservice.service.EventProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.datasource.url=${TRACEABILITY_TEST_DB_URL:jdbc:h2:mem:traceability-grpc;MODE=PostgreSQL}",
        "spring.datasource.username=${TRACEABILITY_TEST_DB_USERNAME:sa}",
        "spring.datasource.password=${TRACEABILITY_TEST_DB_PASSWORD:}",
        "spring.datasource.driver-class-name=${TRACEABILITY_TEST_DB_DRIVER:org.h2.Driver}",
        "spring.sql.init.mode=${TRACEABILITY_TEST_DB_INIT:always}",
        "spring.sql.init.schema-locations=classpath:db/schema.sql",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "grpc.server.port=0"
})
@DirtiesContext
class TraceabilityGrpcIntegrationTest
{
    @Autowired private GrpcServerLifecycle grpcServer;
    @Autowired private EventProcessingService events;
    @Autowired private Declarables traceabilityTopology;

    @Test
    void servesPreciseQueriesThroughTheRunningGrpcServer() throws Exception
    {
        events.createProduct(new ProductCreatedEvent(UUID.randomUUID(), "ProductCreated", Instant.now(),
                900001L, List.of(new ProductCreatedEvent.ProductPart(
                9_000_001L, 900001L, "GRPC-ANIMAL-A", new BigDecimal("1.25")))));
        events.createProduct(new ProductCreatedEvent(UUID.randomUUID(), "ProductCreated", Instant.now(),
                900002L, List.of(new ProductCreatedEvent.ProductPart(
                9_000_002L, 900001L, "GRPC-ANIMAL-B", new BigDecimal("1.25")))));

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", grpcServer.getPort())
                .usePlaintext().build();
        try
        {
            var client = TraceabilityServiceGrpc.newBlockingStub(channel).withDeadlineAfter(5, TimeUnit.SECONDS);
            assertEquals(List.of("GRPC-ANIMAL-A"), client.getAnimalsForProduct(
                    ProductRequest.newBuilder().setProductId(900001).build()).getRegistrationNumbersList());
            assertEquals(List.of("GRPC-ANIMAL-B"), client.getAnimalsForProduct(
                    ProductRequest.newBuilder().setProductId(900002).build()).getRegistrationNumbersList());
            assertEquals(List.of(900002), client.getProductsForAnimal(
                    AnimalRequest.newBuilder().setRegistrationNumber("GRPC-ANIMAL-B").build()).getProductIdsList());
            assertEquals(List.of(), client.getAnimalsForProduct(ProductRequest.getDefaultInstance())
                    .getRegistrationNumbersList());
        } finally
        {
            channel.shutdownNow();
            assertTrue(channel.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void declaresAllFourQueuesAsDurableWithoutAutoDelete()
    {
        var queues = traceabilityTopology.getDeclarables().stream()
                .filter(Queue.class::isInstance).map(Queue.class::cast).toList();

        assertEquals(4, queues.size());
        assertTrue(queues.stream().allMatch(Queue::isDurable));
        assertTrue(queues.stream().noneMatch(Queue::isAutoDelete));
        assertEquals(9, traceabilityTopology.getDeclarables().size());
    }
}
