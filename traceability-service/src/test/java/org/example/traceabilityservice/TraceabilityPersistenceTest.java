package org.example.traceabilityservice;

import jakarta.persistence.EntityManager;
import org.example.traceabilityservice.entity.PartType;
import org.example.traceabilityservice.entity.Product;
import org.example.traceabilityservice.exception.IncompleteTraceabilityException;
import org.example.traceabilityservice.messaging.*;
import org.example.traceabilityservice.repository.*;
import org.example.traceabilityservice.service.EventProcessingService;
import org.example.traceabilityservice.service.TraceabilityQueryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:traceability;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({EventProcessingService.class, TraceabilityQueryService.class, TraceabilityEventConsumer.class,
        TraceabilityPersistenceTest.TestConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TraceabilityPersistenceTest
{
    private static final BigDecimal WEIGHT = new BigDecimal("1.25");

    @Autowired private EventProcessingService events;
    @Autowired private TraceabilityEventConsumer consumer;
    @Autowired private TraceabilityQueryService queries;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AnimalRepository animalRepo;
    @Autowired private PartRepository partRepo;
    @Autowired private TrayRepository trayRepo;
    @Autowired private ProductRepository productRepo;
    @Autowired private ProcessedEventRepository processedRepo;
    @Autowired private FailedEventRepository failedRepo;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;

    @TestConfiguration
    static class TestConfig
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
            productRepo.deleteAll();
            trayRepo.deleteAll();
            partRepo.deleteAll();
            animalRepo.deleteAll();
            processedRepo.deleteAll();
            failedRepo.deleteAll();
        });
    }

    @Test
    void registersAnAnimalAndSkipsDuplicateEvent()
    {
        AnimalRegisteredEvent event = animal("ANIMAL-A");

        events.registerAnimal(event);
        events.registerAnimal(event);

        assertTrue(animalRepo.existsById("ANIMAL-A"));
        assertEquals(1, animalRepo.count());
        assertEquals(1, processedRepo.count());
        assertTrue(processedRepo.existsById(event.eventId()));
    }

    @Test
    void differentEventsForTheSameAnimalDoNotDuplicateItsReference()
    {
        events.registerAnimal(animal("ANIMAL-A"));
        events.registerAnimal(animal("ANIMAL-A"));

        assertEquals(1, animalRepo.count());
        assertEquals(2, processedRepo.count());
    }

    @Test
    void registersPartBeforeAnimalEventAndEnrichesWithoutDuplicates()
    {
        PartRegisteredEvent part = part(101L, "ANIMAL-A", PartType.LEG);
        events.registerPart(part);
        events.registerPart(part);
        events.registerAnimal(animal("ANIMAL-A"));

        assertEquals(1, animalRepo.count());
        assertEquals(1, partRepo.count());
        assertEquals(2, processedRepo.count());
        inTransaction(() -> {
            var stored = partRepo.findById(101L).orElseThrow();
            assertEquals("ANIMAL-A", stored.getAnimal().getRegistrationNumber());
            assertEquals(WEIGHT, stored.getWeight());
            assertEquals(PartType.LEG, stored.getType());
        });
    }

    @Test
    void receivesTrayBeforePartAndAnimalEvents()
    {
        TraySealedEvent tray = tray(10L, PartType.LEG,
                trayPart(101L, "ANIMAL-A"), trayPart(102L, "ANIMAL-B"));
        events.sealTray(tray);
        events.sealTray(tray);
        events.registerPart(part(101L, "ANIMAL-A", PartType.LEG));
        events.registerAnimal(animal("ANIMAL-A"));

        assertEquals(1, trayRepo.count());
        assertEquals(2, partRepo.count());
        assertEquals(2, animalRepo.count());
        assertEquals(3, processedRepo.count());
        inTransaction(() -> assertEquals(2, trayRepo.findById(10L).orElseThrow().getParts().size()));
    }

    @Test
    void productFirstEstablishesTraceabilityAndLaterEventsPreserveIt()
    {
        ProductCreatedEvent product = product(1L, productPart(101L, 10L, "ANIMAL-A"));
        events.createProduct(product);

        assertEquals(List.of("ANIMAL-A"), queries.getAnimalsForProduct(1L));
        assertEquals(List.of(1L), queries.getProductsForAnimal("ANIMAL-A"));
        inTransaction(() -> assertNull(partRepo.findById(101L).orElseThrow().getType()));

        events.sealTray(tray(10L, PartType.LEG,
                trayPart(101L, "ANIMAL-A"), trayPart(102L, "ANIMAL-B")));
        events.registerPart(part(101L, "ANIMAL-A", PartType.LEG));
        events.registerAnimal(animal("ANIMAL-A"));
        events.createProduct(product);

        assertEquals(List.of("ANIMAL-A"), queries.getAnimalsForProduct(1L));
        assertEquals(List.of(), queries.getProductsForAnimal("ANIMAL-B"));
        assertEquals(4, processedRepo.count());
        assertEquals(1, productRepo.count());
        assertEquals(1, trayRepo.count());
        inTransaction(() -> assertEquals(PartType.LEG, partRepo.findById(101L).orElseThrow().getType()));
    }

    @Test
    void productsUsingDifferentPartsFromOneTrayTraceOnlyTheirOwnAnimals()
    {
        events.sealTray(tray(10L, PartType.LEG,
                trayPart(101L, "ANIMAL-A"), trayPart(102L, "ANIMAL-B")));
        events.createProduct(product(1L, productPart(101L, 10L, "ANIMAL-A")));
        events.createProduct(product(2L, productPart(102L, 10L, "ANIMAL-B")));

        assertEquals(List.of("ANIMAL-A"), queries.getAnimalsForProduct(1L));
        assertEquals(List.of("ANIMAL-B"), queries.getAnimalsForProduct(2L));
        assertEquals(List.of(1L), queries.getProductsForAnimal("ANIMAL-A"));
        assertEquals(List.of(2L), queries.getProductsForAnimal("ANIMAL-B"));
        inTransaction(() -> {
            assertEquals(1, productRepo.findById(1L).orElseThrow().getParts().size());
            assertEquals(1, productRepo.findById(2L).orElseThrow().getParts().size());
            assertEquals(1, productRepo.findById(1L).orElseThrow().getTrays().size());
            assertEquals(1, productRepo.findById(2L).orElseThrow().getTrays().size());
        });
    }

    @Test
    void mixedProductsAcrossTraysReturnDistinctAnimalsAndProducts()
    {
        events.createProduct(product(1L,
                productPart(101L, 10L, "ANIMAL-A"),
                productPart(102L, 10L, "ANIMAL-B"),
                productPart(201L, 20L, "ANIMAL-A")));
        events.createProduct(product(2L,
                productPart(103L, 10L, "ANIMAL-A"),
                productPart(202L, 20L, "ANIMAL-A")));

        assertEquals(List.of("ANIMAL-A", "ANIMAL-B"), queries.getAnimalsForProduct(1L));
        assertEquals(List.of("ANIMAL-A"), queries.getAnimalsForProduct(2L));
        assertEquals(List.of(1L, 2L), queries.getProductsForAnimal("ANIMAL-A"));
        assertEquals(List.of(1L), queries.getProductsForAnimal("ANIMAL-B"));
        assertEquals(2, trayRepo.count());
        inTransaction(() -> assertEquals(2, productRepo.findById(1L).orElseThrow().getTrays().size()));
    }

    @Test
    void repeatedProductWithANewEventIdDoesNotDuplicateRelationships()
    {
        events.createProduct(product(1L, productPart(101L, 10L, "ANIMAL-A")));
        events.createProduct(product(1L, productPart(101L, 10L, "ANIMAL-A")));

        assertEquals(1, productRepo.count());
        assertEquals(1, partRepo.count());
        assertEquals(2, processedRepo.count());
        inTransaction(() -> {
            assertEquals(1, productRepo.findById(1L).orElseThrow().getParts().size());
            assertEquals(1, trayRepo.findById(10L).orElseThrow().getParts().size());
        });
    }

    @Test
    void conflictingEventRollsBackReferencesRelationshipsAndProcessedMarker()
    {
        events.registerPart(part(102L, "ANIMAL-A", PartType.LEG));
        ProductCreatedEvent conflicting = product(77L,
                productPart(101L, 20L, "ANIMAL-C"),
                productPart(102L, 20L, "ANIMAL-B"));

        assertThrows(IllegalArgumentException.class, () -> events.createProduct(conflicting));

        assertFalse(processedRepo.existsById(conflicting.eventId()));
        assertEquals(1, processedRepo.count());
        assertEquals(1, animalRepo.count());
        assertEquals(1, partRepo.count());
        assertEquals(0, trayRepo.count());
        assertEquals(0, productRepo.count());
        inTransaction(() -> assertEquals("ANIMAL-A",
                partRepo.findById(102L).orElseThrow().getAnimal().getRegistrationNumber()));
    }

    @Test
    void aPartCannotBeAssignedToAnotherProduct()
    {
        events.createProduct(product(1L, productPart(101L, 10L, "ANIMAL-A")));
        ProductCreatedEvent conflicting = product(2L, productPart(101L, 10L, "ANIMAL-A"));

        assertThrows(IllegalArgumentException.class, () -> events.createProduct(conflicting));

        assertEquals(1, productRepo.count());
        assertFalse(processedRepo.existsById(conflicting.eventId()));
        assertEquals(List.of(1L), queries.getProductsForAnimal("ANIMAL-A"));
    }

    @Test
    void laterTrayEventCannotMoveAPartToAnotherTray()
    {
        events.createProduct(product(1L, productPart(101L, 10L, "ANIMAL-A")));
        TraySealedEvent conflicting = tray(20L, PartType.LEG, trayPart(101L, "ANIMAL-A"));

        assertThrows(IllegalArgumentException.class, () -> events.sealTray(conflicting));

        assertEquals(1, trayRepo.count());
        assertFalse(processedRepo.existsById(conflicting.eventId()));
        assertEquals(List.of("ANIMAL-A"), queries.getAnimalsForProduct(1L));
    }

    @Test
    void duplicatePartsWithinOneProductAreRejectedAtomically()
    {
        ProductCreatedEvent invalid = product(1L,
                productPart(101L, 10L, "ANIMAL-A"), productPart(101L, 10L, "ANIMAL-A"));

        assertThrows(IllegalArgumentException.class, () -> events.createProduct(invalid));

        assertEquals(0, processedRepo.count());
        assertEquals(0, animalRepo.count());
        assertEquals(0, partRepo.count());
        assertEquals(0, productRepo.count());
    }

    @Test
    void missingAnimalRegistrationNumberIsNotRecordedAsProcessed()
    {
        AnimalRegisteredEvent invalid = animal(" ");

        assertThrows(IllegalArgumentException.class, () -> events.registerAnimal(invalid));

        assertEquals(0, animalRepo.count());
        assertEquals(0, processedRepo.count());
    }

    @Test
    void emptyProductIsNotRecordedAsProcessed()
    {
        ProductCreatedEvent invalid = product(1L);

        assertThrows(IllegalArgumentException.class, () -> events.createProduct(invalid));

        assertEquals(0, productRepo.count());
        assertEquals(0, processedRepo.count());
    }

    @Test
    void inconsistentTrayWeightIsNotRecordedAsProcessed()
    {
        TraySealedEvent invalid = new TraySealedEvent(UUID.randomUUID(), "TraySealed", Instant.now(),
                10L, PartType.LEG, new BigDecimal("99.00"), List.of(trayPart(101L, "ANIMAL-A")));

        assertThrows(IllegalArgumentException.class, () -> events.sealTray(invalid));

        assertEquals(0, trayRepo.count());
        assertEquals(0, partRepo.count());
        assertEquals(0, processedRepo.count());
    }

    @Test
    void malformedDeliveriesAreRetainedOnceInTheLocalFailureTable()
    {
        consumer.consumeProductCreated("{not JSON");
        consumer.consumeProductCreated("{not JSON");

        assertEquals(1, failedRepo.count());
        assertEquals("{not JSON", failedRepo.findAll().getFirst().getPayload());
        assertEquals(0, processedRepo.count());
        assertEquals(0, productRepo.count());
    }

    @Test
    void conflictingDeliveryIsRetainedAfterItsProjectionTransactionRollsBack()
    {
        events.createProduct(product(1L, productPart(101L, 10L, "ANIMAL-A")));
        ProductCreatedEvent conflicting = product(2L, productPart(101L, 10L, "ANIMAL-A"));
        String payload = objectMapper.writeValueAsString(conflicting);

        assertDoesNotThrow(() -> consumer.consumeProductCreated(payload));

        assertEquals(1, failedRepo.count());
        assertEquals(payload, failedRepo.findAll().getFirst().getPayload());
        assertEquals(1, productRepo.count());
        assertFalse(processedRepo.existsById(conflicting.eventId()));
        assertEquals(List.of(1L), queries.getProductsForAnimal("ANIMAL-A"));
    }

    @Test
    void legacyTrayOnlyProductIsReportedAsIncompleteUntilOriginalProductEventArrives()
    {
        events.sealTray(tray(10L, PartType.LEG,
                trayPart(101L, "ANIMAL-A"), trayPart(102L, "ANIMAL-B")));
        inTransaction(() -> {
            Product legacy = new Product();
            legacy.setId(7L);
            legacy.getTrays().add(trayRepo.findById(10L).orElseThrow());
            entityManager.persist(legacy);
        });

        assertThrows(IncompleteTraceabilityException.class, () -> queries.getAnimalsForProduct(7L));
        assertThrows(IncompleteTraceabilityException.class, () -> queries.getProductsForAnimal("ANIMAL-A"));

        events.createProduct(product(7L, productPart(101L, 10L, "ANIMAL-A")));

        assertEquals(List.of("ANIMAL-A"), queries.getAnimalsForProduct(7L));
        assertEquals(List.of(7L), queries.getProductsForAnimal("ANIMAL-A"));
        assertEquals(List.of(), queries.getProductsForAnimal("ANIMAL-B"));
        inTransaction(() -> assertEquals(1, productRepo.findById(7L).orElseThrow().getTrays().size()));
    }

    @Test
    void unknownAndDefaultRequestsReturnEmptyLists()
    {
        assertEquals(List.of(), queries.getAnimalsForProduct(999L));
        assertEquals(List.of(), queries.getAnimalsForProduct(0L));
        assertEquals(List.of(), queries.getAnimalsForProduct(-1L));
        assertEquals(List.of(), queries.getProductsForAnimal("UNKNOWN"));
        assertEquals(List.of(), queries.getProductsForAnimal(""));
        assertEquals(List.of(), queries.getProductsForAnimal(" "));
    }

    @Test
    void identifiersRemainLongValuesInTheDatabase()
    {
        long productId = (long) Integer.MAX_VALUE + 1;
        events.createProduct(product(productId,
                productPart(4_000_000_001L, 4_000_000_002L, "ANIMAL-A")));

        assertTrue(productRepo.existsById(productId));
        assertEquals(List.of("ANIMAL-A"), queries.getAnimalsForProduct(productId));
        assertEquals(List.of(productId), queries.getProductsForAnimal("ANIMAL-A"));
    }

    @Test
    void higherPrecisionRegistrationWeightMatchesLaterRoundedTrayWeight()
    {
        events.registerPart(new PartRegisteredEvent(UUID.randomUUID(), "PartRegistered", Instant.now(),
                101L, "ANIMAL-A", PartType.LEG, new BigDecimal("1.254")));
        events.sealTray(tray(10L, PartType.LEG, trayPart(101L, "ANIMAL-A")));
        events.createProduct(product(1L, productPart(101L, 10L, "ANIMAL-A")));

        assertEquals(List.of("ANIMAL-A"), queries.getAnimalsForProduct(1L));
        inTransaction(() -> assertEquals(WEIGHT, partRepo.findById(101L).orElseThrow().getWeight()));
    }

    @Test
    void lateHigherPrecisionPartRegistrationDoesNotConflictWithRoundedProductWeight()
    {
        events.createProduct(product(1L, productPart(101L, 10L, "ANIMAL-A")));
        events.registerPart(new PartRegisteredEvent(UUID.randomUUID(), "PartRegistered", Instant.now(),
                101L, "ANIMAL-A", PartType.LEG, new BigDecimal("1.245")));

        assertEquals(List.of("ANIMAL-A"), queries.getAnimalsForProduct(1L));
        inTransaction(() -> {
            var part = partRepo.findById(101L).orElseThrow();
            assertEquals(WEIGHT, part.getWeight());
            assertEquals(PartType.LEG, part.getType());
        });
    }

    private void inTransaction(Runnable action)
    {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
    }

    private AnimalRegisteredEvent animal(String registrationNumber)
    {
        return new AnimalRegisteredEvent(UUID.randomUUID(), "AnimalRegistered", Instant.now(), registrationNumber);
    }

    private PartRegisteredEvent part(long id, String animal, PartType type)
    {
        return new PartRegisteredEvent(UUID.randomUUID(), "PartRegistered", Instant.now(), id, animal, type, WEIGHT);
    }

    private TraySealedEvent.TrayPart trayPart(long id, String animal)
    {
        return new TraySealedEvent.TrayPart(id, animal, WEIGHT);
    }

    private TraySealedEvent tray(long id, PartType type, TraySealedEvent.TrayPart... parts)
    {
        return new TraySealedEvent(UUID.randomUUID(), "TraySealed", Instant.now(), id, type,
                WEIGHT.multiply(BigDecimal.valueOf(parts.length)), List.of(parts));
    }

    private ProductCreatedEvent.ProductPart productPart(long id, long tray, String animal)
    {
        return new ProductCreatedEvent.ProductPart(id, tray, animal, WEIGHT);
    }

    private ProductCreatedEvent product(long id, ProductCreatedEvent.ProductPart... parts)
    {
        return new ProductCreatedEvent(UUID.randomUUID(), "ProductCreated", Instant.now(), id, List.of(parts));
    }
}
