package org.example.traceabilityservice;

import grpc.AnimalRequest;
import grpc.ProductRequest;
import grpc.TraceabilityServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import jakarta.persistence.EntityManager;
import org.example.traceabilityservice.entity.PartType;
import org.example.traceabilityservice.entity.Tray;
import org.example.traceabilityservice.grpcservice.GrpcServerLifecycle;
import org.example.traceabilityservice.messaging.*;
import org.example.traceabilityservice.repository.*;
import org.example.traceabilityservice.service.EventProcessingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.datasource.url=${TRACEABILITY_TEST_DB_URL}",
        "spring.datasource.username=${TRACEABILITY_TEST_DB_USERNAME}",
        "spring.datasource.password=${TRACEABILITY_TEST_DB_PASSWORD}",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.datasource.hikari.maximum-pool-size=10",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never",
        "spring.rabbitmq.listener.simple.auto-startup=false",
        "grpc.server.port=0"
})
@EnabledIfEnvironmentVariable(named = "TRACEABILITY_TEST_DB_URL", matches = ".+")
@DirtiesContext
class TraceabilityPostgresConcurrencyTest
{
    private static final BigDecimal WEIGHT = new BigDecimal("1.25");
    private static final AtomicLong IDS = new AtomicLong(
            1_000_000_000L + Math.floorMod(System.nanoTime(), 500_000_000L));

    @Autowired private EventProcessingService events;
    @Autowired private TraceabilityEventConsumer consumer;
    @Autowired private AnimalRepository animalRepo;
    @Autowired private PartRepository partRepo;
    @Autowired private TrayRepository trayRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private ProcessedEventRepository processedRepo;
    @Autowired private FailedEventRepository failedRepo;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private GrpcServerLifecycle grpcServer;

    @Test
    void differentEventTypesSharePreviouslyUnknownReferencesWithoutFailures() throws Exception
    {
        long base = IDS.getAndAdd(100);
        String animal = animalNumber();
        AnimalRegisteredEvent registration = animal(animal);
        PartRegisteredEvent part = part(base, animal);
        TraySealedEvent tray = tray(base + 1, trayPart(base, animal));
        ProductCreatedEvent product = product(base + 2, productPart(base, base + 1, animal));

        commitWhileOthersWait(() -> events.registerAnimal(registration),
                () -> consumer.consumePartRegistered(json(part)),
                () -> consumer.consumeTraySealed(json(tray)),
                () -> consumer.consumeProductCreated(json(product)));

        assertProcessed(registration.eventId(), part.eventId(), tray.eventId(), product.eventId());
        assertTrue(animalRepo.existsById(animal));
        inTransaction(() -> {
            var stored = partRepo.findById(base).orElseThrow();
            assertEquals(animal, stored.getAnimal().getRegistrationNumber());
            assertEquals(WEIGHT, stored.getWeight());
            assertEquals(PartType.LEG, stored.getType());
            assertEquals(Set.of(base), trayRepo.findById(base + 1).orElseThrow().getParts().stream()
                    .map(item -> item.getId()).collect(Collectors.toSet()));
            assertEquals(Set.of(base), productRepo.findById(base + 2).orElseThrow().getParts().stream()
                    .map(item -> item.getId()).collect(Collectors.toSet()));
        });
        assertTraceability(base + 2, animal, base + 2);
    }

    @Test
    void missingPartIsCreatedOnceWhenAnimalAndTrayAlreadyExist() throws Exception
    {
        long base = IDS.getAndAdd(100);
        String animal = animalNumber();
        events.registerAnimal(animal(animal));
        inTransaction(() -> {
            Tray reference = new Tray();
            reference.setId(base + 1);
            entityManager.persist(reference);
        });
        PartRegisteredEvent part = part(base, animal);
        TraySealedEvent tray = tray(base + 1, trayPart(base, animal));
        ProductCreatedEvent product = product(base + 2, productPart(base, base + 1, animal));

        commitWhileOthersWait(() -> events.registerPart(part),
                () -> consumer.consumeTraySealed(json(tray)),
                () -> consumer.consumeProductCreated(json(product)));

        assertProcessed(part.eventId(), tray.eventId(), product.eventId());
        inTransaction(() -> {
            assertEquals(1, trayRepo.findById(base + 1).orElseThrow().getParts().size());
            assertEquals(PartType.LEG, partRepo.findById(base).orElseThrow().getType());
        });
        assertTraceability(base + 2, animal, base + 2);
    }

    @Test
    void missingTrayIsSharedWithoutLosingPartsOrProductRelationships() throws Exception
    {
        long base = IDS.getAndAdd(100);
        String firstAnimal = animalNumber();
        String secondAnimal = animalNumber();
        events.registerPart(part(base, firstAnimal));
        events.registerPart(part(base + 1, secondAnimal));
        TraySealedEvent tray = tray(base + 2,
                trayPart(base, firstAnimal), trayPart(base + 1, secondAnimal));
        ProductCreatedEvent first = product(base + 3, productPart(base, base + 2, firstAnimal));
        ProductCreatedEvent second = product(base + 4, productPart(base + 1, base + 2, secondAnimal));

        commitWhileOthersWait(() -> events.sealTray(tray),
                () -> consumer.consumeProductCreated(json(first)),
                () -> consumer.consumeProductCreated(json(second)));

        assertProcessed(tray.eventId(), first.eventId(), second.eventId());
        inTransaction(() -> assertEquals(Set.of(base, base + 1),
                trayRepo.findById(base + 2).orElseThrow().getParts().stream()
                        .map(item -> item.getId()).collect(Collectors.toSet())));
        assertTraceability(base + 3, firstAnimal, base + 3);
        assertTraceability(base + 4, secondAnimal, base + 4);
    }

    @Test
    void missingProductIsCreatedOnceForDifferentEventsWithIdenticalComposition() throws Exception
    {
        long base = IDS.getAndAdd(100);
        String animal = animalNumber();
        events.sealTray(tray(base + 1, trayPart(base, animal)));
        ProductCreatedEvent first = product(base + 2, productPart(base, base + 1, animal));
        ProductCreatedEvent second = product(base + 2, productPart(base, base + 1, animal));

        commitWhileOthersWait(() -> events.createProduct(first),
                () -> consumer.consumeProductCreated(json(second)));

        assertProcessed(first.eventId(), second.eventId());
        inTransaction(() -> {
            var stored = productRepo.findById(base + 2).orElseThrow();
            assertEquals(1, stored.getParts().size());
            assertEquals(1, stored.getTrays().size());
        });
        assertTraceability(base + 2, animal, base + 2);
    }

    @Test
    void concurrentDuplicateEventIsAcknowledgedOnlyAfterTheFirstTransactionCommits() throws Exception
    {
        String animal = animalNumber();
        AnimalRegisteredEvent event = animal(animal);
        String payload = json(event);

        commitWhileOthersWait(() -> events.registerAnimal(event),
                () -> consumer.consumeAnimalRegistered(payload),
                () -> consumer.consumeAnimalRegistered(payload));

        assertProcessed(event.eventId());
        assertEquals(1, processedRepo.findAllById(List.of(event.eventId())).size());
        assertTrue(animalRepo.existsById(animal));
    }

    @Test
    void businessConflictsRollBackTheirClaimsAndNeverOverwriteValidReferences() throws Exception
    {
        long base = IDS.getAndAdd(100);
        String animal = animalNumber();
        String conflictingAnimal = animalNumber();
        events.registerPart(part(base, animal));
        UUID eventId = UUID.randomUUID();
        ProductCreatedEvent conflicting = new ProductCreatedEvent(eventId, "ProductCreated", Instant.now(),
                base + 2, List.of(productPart(base, base + 1, conflictingAnimal)));

        assertThrows(IllegalArgumentException.class, () -> events.createProduct(conflicting));
        assertFalse(processedRepo.existsById(eventId));
        assertFalse(productRepo.existsById(base + 2));
        assertFalse(animalRepo.existsById(conflictingAnimal));

        ProductCreatedEvent corrected = new ProductCreatedEvent(eventId, "ProductCreated", Instant.now(),
                base + 2, List.of(productPart(base, base + 1, animal)));
        events.createProduct(corrected);
        assertProcessed(eventId);

        ProductCreatedEvent wrongOwner = product(base + 3, productPart(base, base + 1, animal));
        assertThrows(IllegalArgumentException.class, () -> events.createProduct(wrongOwner));
        assertFalse(processedRepo.existsById(wrongOwner.eventId()));
        assertFalse(productRepo.existsById(base + 3));

        PartRegisteredEvent wrongWeight = new PartRegisteredEvent(UUID.randomUUID(), "PartRegistered",
                Instant.now(), base, animal, PartType.LEG, new BigDecimal("9.99"));
        consumer.consumePartRegistered(json(wrongWeight));
        assertFalse(processedRepo.existsById(wrongWeight.eventId()));
        assertEquals(1, failedRepo.findAll().stream()
                .filter(failure -> failure.getPayload().contains(wrongWeight.eventId().toString())).count());
        inTransaction(() -> {
            var stored = partRepo.findById(base).orElseThrow();
            assertEquals(animal, stored.getAnimal().getRegistrationNumber());
            assertEquals(WEIGHT, stored.getWeight());
        });
        assertTraceability(base + 2, animal, base + 2);
    }

    private void commitWhileOthersWait(Runnable firstEvent, Runnable... contenders) throws Exception
    {
        CountDownLatch firstWritten = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(contenders.length + 1))
        {
            Future<?> first = executor.submit(() -> inTransaction(() -> {
                firstEvent.run();
                entityManager.flush();
                firstWritten.countDown();
                await(releaseFirst);
            }));
            List<Future<?>> waiting = new ArrayList<>();
            try
            {
                assertTrue(firstWritten.await(15, TimeUnit.SECONDS), "First transaction did not write its references");
                for (Runnable contender : contenders)
                {
                    waiting.add(executor.submit(contender));
                }
                awaitBlockedTransactions(contenders.length);
                assertTrue(waiting.stream().noneMatch(Future::isDone),
                        "Competing events must wait for the first transaction");
            } finally
            {
                releaseFirst.countDown();
            }
            first.get(15, TimeUnit.SECONDS);
            for (Future<?> contender : waiting)
            {
                contender.get(15, TimeUnit.SECONDS);
            }
        }
    }

    private void awaitBlockedTransactions(int expected) throws Exception
    {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline)
        {
            Number waiting = new TransactionTemplate(transactionManager).execute(status ->
                    (Number) entityManager.createNativeQuery("""
                            SELECT COUNT(*) FROM pg_stat_activity
                            WHERE datname = current_database()
                              AND pid <> pg_backend_pid()
                              AND wait_event_type = 'Lock'
                            """, Long.class).getSingleResult());
            if (waiting != null && waiting.intValue() >= expected)
            {
                return;
            }
            Thread.sleep(25);
        }
        fail("Concurrent events did not all reach a PostgreSQL lock wait");
    }

    private void assertProcessed(UUID... eventIds)
    {
        assertEquals(eventIds.length, processedRepo.findAllById(Arrays.asList(eventIds)).size());
        assertTrue(failedRepo.findAll().stream().noneMatch(failure -> Arrays.stream(eventIds)
                .anyMatch(id -> failure.getPayload().contains(id.toString()))));
    }

    private void assertTraceability(long productId, String animal, long... productIds) throws Exception
    {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", grpcServer.getPort())
                .usePlaintext().build();
        try
        {
            var client = TraceabilityServiceGrpc.newBlockingStub(channel).withDeadlineAfter(5, TimeUnit.SECONDS);
            assertEquals(List.of(animal), client.getAnimalsForProduct(ProductRequest.newBuilder()
                    .setProductId(Math.toIntExact(productId)).build()).getRegistrationNumbersList());
            assertEquals(Arrays.stream(productIds).mapToObj(Math::toIntExact).sorted().toList(),
                    client.getProductsForAnimal(AnimalRequest.newBuilder().setRegistrationNumber(animal).build())
                            .getProductIdsList());
        } finally
        {
            channel.shutdownNow();
            assertTrue(channel.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private void inTransaction(Runnable action)
    {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
    }

    private void await(CountDownLatch latch)
    {
        try
        {
            assertTrue(latch.await(15, TimeUnit.SECONDS), "Held transaction was not released");
        } catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private String animalNumber()
    {
        return "PG-RACE-" + UUID.randomUUID();
    }

    private AnimalRegisteredEvent animal(String registrationNumber)
    {
        return new AnimalRegisteredEvent(UUID.randomUUID(), "AnimalRegistered", Instant.now(), registrationNumber);
    }

    private PartRegisteredEvent part(long id, String animal)
    {
        return new PartRegisteredEvent(UUID.randomUUID(), "PartRegistered", Instant.now(), id, animal,
                PartType.LEG, WEIGHT);
    }

    private TraySealedEvent.TrayPart trayPart(long id, String animal)
    {
        return new TraySealedEvent.TrayPart(id, animal, WEIGHT);
    }

    private TraySealedEvent tray(long id, TraySealedEvent.TrayPart... parts)
    {
        return new TraySealedEvent(UUID.randomUUID(), "TraySealed", Instant.now(), id, PartType.LEG,
                WEIGHT.multiply(BigDecimal.valueOf(parts.length)), List.of(parts));
    }

    private ProductCreatedEvent.ProductPart productPart(long id, long trayId, String animal)
    {
        return new ProductCreatedEvent.ProductPart(id, trayId, animal, WEIGHT);
    }

    private ProductCreatedEvent product(long id, ProductCreatedEvent.ProductPart... parts)
    {
        return new ProductCreatedEvent(UUID.randomUUID(), "ProductCreated", Instant.now(), id, List.of(parts));
    }

    private String json(Object event)
    {
        return objectMapper.writeValueAsString(event);
    }
}
