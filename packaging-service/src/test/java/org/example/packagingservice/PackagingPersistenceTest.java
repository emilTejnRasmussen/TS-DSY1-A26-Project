package org.example.packagingservice;

import org.example.packagingservice.dto.CreateProductRequest;
import org.example.packagingservice.dto.ProductResponse;
import org.example.packagingservice.entity.OutboxEvent;
import org.example.packagingservice.entity.PartType;
import org.example.packagingservice.exception.PartAlreadyConsumedException;
import org.example.packagingservice.mapper.PartMapper;
import org.example.packagingservice.mapper.ProductMapper;
import org.example.packagingservice.mapper.TrayMapper;
import org.example.packagingservice.messaging.*;
import org.example.packagingservice.repository.*;
import org.example.packagingservice.service.ProductService;
import org.example.packagingservice.service.TrayService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:packaging",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({TrayEventConsumer.class, ProductService.class, TrayService.class, OutboxService.class,
        ProductMapper.class, TrayMapper.class, PartMapper.class, PackagingPersistenceTest.JsonConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PackagingPersistenceTest
{
    @Autowired private TrayEventConsumer consumer;
    @Autowired private ProductService productService;
    @Autowired private TrayService trayService;
    @Autowired private TrayRepository trayRepo;
    @Autowired private PartRepository partRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private OutboxEventRepository outboxRepo;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PlatformTransactionManager transactionManager;

    @MockitoSpyBean
    private OutboxService outboxService;

    @TestConfiguration
    static class JsonConfig
    {
        @Bean
        ObjectMapper objectMapper()
        {
            return JsonMapper.builder().build();
        }
    }

    @AfterEach
    void cleanUp()
    {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            partRepo.deleteAllInBatch();
            productRepo.deleteAllInBatch();
            trayRepo.deleteAllInBatch();
            outboxRepo.deleteAllInBatch();
        });
    }

    @Test
    void receivesTrayIdempotentlyWithoutResettingConsumedParts()
    {
        TraySealedEvent event = event(10L, PartType.LEG, 101L, 102L);
        String json = objectMapper.writeValueAsString(event);
        consumer.consume(json);
        ProductResponse product = productService.createProduct(
                new CreateProductRequest("Leg", List.of(101L)));

        consumer.consume(json);
        consumer.consume(objectMapper.writeValueAsString(new TraySealedEvent(
                UUID.randomUUID(), event.eventType(), event.occurredAt(), event.trayId(),
                event.partType(), event.totalWeight(), event.parts())));

        assertEquals(1, trayRepo.count());
        assertEquals(2, partRepo.count());
        assertEquals(new BigDecimal("1.25"), trayService.getTray(10L).availableWeight());
        assertEquals(product.id(), productService.getProduct(product.id()).parts().getFirst().productId());
    }

    @Test
    void eventIdCannotCreateASecondTray()
    {
        TraySealedEvent event = event(10L, PartType.LEG, 101L, 102L);
        consumer.consume(objectMapper.writeValueAsString(event));
        consumer.consume(objectMapper.writeValueAsString(new TraySealedEvent(
                event.eventId(), event.eventType(), event.occurredAt(), 20L,
                event.partType(), event.totalWeight(), event.parts())));

        assertEquals(1, trayRepo.count());
        assertFalse(trayRepo.existsById(20L));
    }

    @Test
    void createsProductsFromDifferentPartsOfTheSameTrayAndRecordsTraceability()
    {
        consumer.consume(objectMapper.writeValueAsString(event(10L, PartType.LEG, 101L, 102L)));
        consumer.consume(objectMapper.writeValueAsString(event(20L, PartType.LOIN, 201L, 202L)));

        ProductResponse first = productService.createProduct(
                new CreateProductRequest("Mixed pack", List.of(201L, 101L)));
        ProductResponse second = productService.createProduct(
                new CreateProductRequest("Leg pack", List.of(102L)));

        assertEquals(new BigDecimal("2.50"), first.totalWeight());
        assertEquals(new BigDecimal("1.25"), second.totalWeight());
        assertEquals(2, productService.getProducts(PageRequest.of(0, 10)).getTotalElements());
        assertEquals(2, outboxRepo.count());
        assertEquals(BigDecimal.ZERO, trayService.getTray(10L).availableWeight());
        assertEquals(1, trayService.getTrays(null, true, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(2, trayService.getTrays(null, false, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(0, trayService.getTrays(PartType.LEG, true, PageRequest.of(0, 10)).getTotalElements());
        assertEquals(1, trayService.getTrays(PartType.LEG, false, PageRequest.of(0, 10)).getTotalElements());

        OutboxEvent stored = outboxRepo.findAll().stream()
                .filter(outbox -> objectMapper.readValue(outbox.getPayload(), ProductCreatedEvent.class)
                        .productId().equals(first.id()))
                .findFirst().orElseThrow();
        ProductCreatedEvent outgoing = objectMapper.readValue(stored.getPayload(), ProductCreatedEvent.class);
        assertEquals(stored.getId(), outgoing.eventId());
        assertTrue(java.time.Duration.between(stored.getOccurredAt(), outgoing.occurredAt())
                .abs().toNanos() < 1000);
        assertEquals("ProductCreated", outgoing.eventType());
        assertEquals("product.created", stored.getRoutingKey());
        assertFalse(stored.isPublished());
        assertNull(stored.getPublishedAt());
        assertEquals(List.of(10L, 20L), outgoing.parts().stream().map(ProductCreatedEvent.ProductPart::trayId).toList());
        assertEquals(List.of("DK-101", "DK-201"), outgoing.parts().stream()
                .map(ProductCreatedEvent.ProductPart::animalRegistrationNumber).toList());
    }

    @Test
    void rollsBackProductInventoryAndOutboxTogether()
    {
        consumer.consume(objectMapper.writeValueAsString(event(10L, PartType.LEG, 101L, 102L)));
        OutboxService target = AopTestUtils.getUltimateTargetObject(outboxService);
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("Failure after saving outbox");
        }).when(target).recordProductCreated(any());

        assertThrows(IllegalStateException.class, () -> productService.createProduct(
                new CreateProductRequest("Leg pack", List.of(101L))));

        assertEquals(0, productRepo.count());
        assertEquals(0, outboxRepo.count());
        assertEquals(new BigDecimal("2.50"), trayService.getTray(10L).availableWeight());
    }

    @Test
    void outboxRequiresAnExistingTransaction()
    {
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,
                () -> outboxService.recordProductCreated(new org.example.packagingservice.entity.Product()));
    }

    @Test
    void rejectsInconsistentWeightsWithoutPersistingInventory()
    {
        TraySealedEvent event = event(10L, PartType.LEG, 101L, 102L);
        String json = objectMapper.writeValueAsString(new TraySealedEvent(
                event.eventId(), event.eventType(), event.occurredAt(), event.trayId(),
                event.partType(), new BigDecimal("9.00"), event.parts()));

        assertThrows(IllegalArgumentException.class, () -> consumer.consume(json));
        assertEquals(0, trayRepo.count());
        assertEquals(0, partRepo.count());
    }

    @Test
    void conflictingPartIdCannotOverwriteExistingInventory()
    {
        consumer.consume(objectMapper.writeValueAsString(event(10L, PartType.LEG, 101L, 102L)));

        assertThrows(RuntimeException.class, () -> consumer.consume(objectMapper.writeValueAsString(
                event(20L, PartType.LOIN, 101L, 201L))));

        assertEquals(1, trayRepo.count());
        assertEquals(2, partRepo.count());
        assertEquals(PartType.LEG, trayService.getTray(10L).type());
    }

    @Test
    void concurrentProductsCannotConsumeTheSamePart() throws Exception
    {
        consumer.consume(objectMapper.writeValueAsString(event(10L, PartType.LEG, 101L, 102L)));
        CountDownLatch firstCreated = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);

        try (var executor = Executors.newFixedThreadPool(2))
        {
            var first = executor.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                productService.createProduct(new CreateProductRequest("First pack", List.of(101L)));
                firstCreated.countDown();
                assertDoesNotThrow(() -> assertTrue(releaseFirst.await(5, TimeUnit.SECONDS)));
                return true;
            }));
            var second = executor.submit(() -> {
                assertTrue(firstCreated.await(5, TimeUnit.SECONDS));
                secondStarted.countDown();
                try
                {
                    productService.createProduct(new CreateProductRequest("Second pack", List.of(101L)));
                    return true;
                } catch (PartAlreadyConsumedException e)
                {
                    return false;
                }
            });

            try
            {
                assertTrue(secondStarted.await(5, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> second.get(200, TimeUnit.MILLISECONDS));
            } finally
            {
                releaseFirst.countDown();
            }

            assertTrue(first.get(10, TimeUnit.SECONDS));
            assertFalse(second.get(10, TimeUnit.SECONDS));
        }

        assertEquals(1, productRepo.count());
        assertEquals(1, outboxRepo.count());
    }

    private TraySealedEvent event(long trayId, PartType type, long firstPartId, long secondPartId)
    {
        return new TraySealedEvent(UUID.randomUUID(), "TraySealed", Instant.now(), trayId,
                type, new BigDecimal("2.50"), List.of(
                new TraySealedEvent.TrayPart(firstPartId, "DK-" + firstPartId, new BigDecimal("1.25")),
                new TraySealedEvent.TrayPart(secondPartId, "DK-" + secondPartId, new BigDecimal("1.25"))));
    }
}
