package org.example.animalprocessingservice;

import org.example.animalprocessingservice.dto.CreateTrayRequest;
import org.example.animalprocessingservice.dto.RegisterPartRequest;
import org.example.animalprocessingservice.entity.PartType;
import org.example.animalprocessingservice.entity.Tray;
import org.example.animalprocessingservice.entity.TrayStatus;
import org.example.animalprocessingservice.exception.PartAlreadyAssignedException;
import org.example.animalprocessingservice.mapper.PartMapper;
import org.example.animalprocessingservice.mapper.TrayMapper;
import org.example.animalprocessingservice.messaging.*;
import org.example.animalprocessingservice.repository.*;
import org.example.animalprocessingservice.service.PartService;
import org.example.animalprocessingservice.service.TrayService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:processing",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({AnimalEventConsumer.class, PartService.class, TrayService.class, OutboxService.class,
        PartMapper.class, TrayMapper.class, ProcessingPersistenceTest.JsonConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ProcessingPersistenceTest
{
    @Autowired private AnimalEventConsumer consumer;
    @Autowired private PartService partService;
    @Autowired private TrayService trayService;
    @Autowired private AnimalReferenceRepository animalRepo;
    @Autowired private PartRepository partRepo;
    @Autowired private TrayRepository trayRepo;
    @Autowired private OutboxEventRepository outboxRepo;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PlatformTransactionManager transactionManager;
    @MockitoSpyBean private OutboxService outboxService;

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
            trayRepo.deleteAllInBatch();
            animalRepo.deleteAllInBatch();
            outboxRepo.deleteAllInBatch();
        });
    }

    @Test
    void duplicateAnimalDeliveryCreatesOneReferenceUsableForPartRegistration()
    {
        String json = objectMapper.writeValueAsString(new AnimalRegisteredEvent(
                UUID.randomUUID(), "AnimalRegistered", Instant.now(), "ANIMAL-A"));
        consumer.consume(json);
        consumer.consume(json);
        var part = partService.registerPart(new RegisterPartRequest("ANIMAL-A", PartType.LEG,
                new BigDecimal("1.25")));

        assertEquals(1, animalRepo.count());
        assertEquals(1, partRepo.count());
        assertEquals(1, outboxRepo.count());
        assertEquals("ANIMAL-A", partService.getPart(part.id()).animalRegistrationNumber());
        assertEquals("part.registered", outboxRepo.findAll().get(0).getRoutingKey());
    }

    @Test
    void registersPartsFillsTrayToCapacityAndPersistsSealedEvent()
    {
        receiveAnimal();
        var first = registerPart("0.10");
        var second = registerPart("0.20");
        var tray = trayService.createTray(new CreateTrayRequest(PartType.LEG, new BigDecimal("0.30")));
        trayService.addPart(tray.id(), first.id());
        trayService.addPart(tray.id(), second.id());
        trayService.sealTray(tray.id());

        var stored = trayService.getTray(tray.id());
        assertEquals(TrayStatus.SEALED, stored.status());
        assertEquals(new BigDecimal("0.30"), stored.currentWeight());
        assertNotNull(stored.sealedAt());
        assertEquals(tray.id(), partService.getPart(first.id()).trayId());
        assertEquals(3, outboxRepo.count());

        var outbox = outboxRepo.findAll().stream()
                .filter(event -> event.getEventType().equals("TraySealed")).findFirst().orElseThrow();
        var event = objectMapper.readValue(outbox.getPayload(), TraySealedEvent.class);
        assertEquals(stored.id(), event.trayId());
        assertEquals(outbox.getId(), event.eventId());
        assertEquals(new BigDecimal("0.30"), event.totalWeight());
        assertEquals(Set.of(first.id(), second.id()), event.parts().stream()
                .map(TraySealedEvent.TrayPart::partId).collect(Collectors.toSet()));
        assertFalse(outbox.isPublished());
    }

    @Test
    void removingPartClearsItsPersistedTrayAndAllowsReassignment()
    {
        receiveAnimal();
        var part = registerPart("1.25");
        var first = trayService.createTray(new CreateTrayRequest(PartType.LEG, new BigDecimal("2.50")));
        var second = trayService.createTray(new CreateTrayRequest(PartType.LEG, new BigDecimal("2.50")));
        trayService.addPart(first.id(), part.id());
        trayService.removePart(first.id(), part.id());

        assertNull(partService.getPart(part.id()).trayId());
        assertEquals(BigDecimal.ZERO, trayService.getTray(first.id()).currentWeight());
        trayService.addPart(second.id(), part.id());
        assertEquals(second.id(), partService.getPart(part.id()).trayId());
    }

    @Test
    void partAndOutboxAreRolledBackTogether()
    {
        receiveAnimal();
        OutboxService target = AopTestUtils.getUltimateTargetObject(outboxService);
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("Failure after storing outbox");
        }).when(target).recordPartRegistered(any());

        assertThrows(IllegalStateException.class, () -> registerPart("1.25"));

        assertEquals(0, partRepo.count());
        assertEquals(0, outboxRepo.count());
        assertEquals(1, animalRepo.count());
    }

    @Test
    void failedOutboxRollsBackTraySealing()
    {
        receiveAnimal();
        var part = registerPart("1.25");
        var tray = trayService.createTray(new CreateTrayRequest(PartType.LEG, new BigDecimal("2.50")));
        trayService.addPart(tray.id(), part.id());
        OutboxService target = AopTestUtils.getUltimateTargetObject(outboxService);
        doAnswer(invocation -> {
            invocation.callRealMethod();
            throw new IllegalStateException("Failure after storing outbox");
        }).when(target).recordTraySealed(any());

        assertThrows(IllegalStateException.class, () -> trayService.sealTray(tray.id()));

        assertEquals(TrayStatus.OPEN, trayService.getTray(tray.id()).status());
        assertNull(trayService.getTray(tray.id()).sealedAt());
        assertEquals(1, outboxRepo.count());
    }

    @Test
    void recordingOutboxRequiresAnExistingTransaction()
    {
        assertThrows(org.springframework.transaction.IllegalTransactionStateException.class,
                () -> outboxService.recordTraySealed(new Tray()));
    }

    @Test
    void concurrentAssignmentsCannotPutOnePartInTwoTrays() throws Exception
    {
        receiveAnimal();
        var part = registerPart("1.25");
        var firstTray = trayService.createTray(new CreateTrayRequest(PartType.LEG, new BigDecimal("2.50")));
        var secondTray = trayService.createTray(new CreateTrayRequest(PartType.LEG, new BigDecimal("2.50")));
        CountDownLatch firstAssigned = new CountDownLatch(1);
        CountDownLatch secondStarted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try
        {
            var first = executor.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                trayService.addPart(firstTray.id(), part.id());
                firstAssigned.countDown();
                assertDoesNotThrow(() -> assertTrue(releaseFirst.await(5, TimeUnit.SECONDS)));
                return true;
            }));
            var second = executor.submit(() -> {
                assertTrue(firstAssigned.await(5, TimeUnit.SECONDS));
                secondStarted.countDown();
                try
                {
                    trayService.addPart(secondTray.id(), part.id());
                    return true;
                } catch (PartAlreadyAssignedException e)
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
            assertEquals(firstTray.id(), partService.getPart(part.id()).trayId());
            assertTrue(trayService.getTray(secondTray.id()).partIds().isEmpty());
        } finally
        {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private void receiveAnimal()
    {
        consumer.consume(objectMapper.writeValueAsString(new AnimalRegisteredEvent(
                UUID.randomUUID(), "AnimalRegistered", Instant.now(), "ANIMAL-A")));
    }

    private org.example.animalprocessingservice.dto.PartResponse registerPart(String weight)
    {
        return partService.registerPart(new RegisterPartRequest("ANIMAL-A", PartType.LEG, new BigDecimal(weight)));
    }
}
