package org.example.animalprocessingservice.service;

import org.example.animalprocessingservice.dto.CreateTrayRequest;
import org.example.animalprocessingservice.entity.Part;
import org.example.animalprocessingservice.entity.PartType;
import org.example.animalprocessingservice.entity.Tray;
import org.example.animalprocessingservice.entity.TrayStatus;
import org.example.animalprocessingservice.exception.*;
import org.example.animalprocessingservice.mapper.TrayMapper;
import org.example.animalprocessingservice.messaging.OutboxService;
import org.example.animalprocessingservice.repository.PartRepository;
import org.example.animalprocessingservice.repository.TrayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrayServiceTest
{
    @Mock private TrayRepository trayRepo;
    @Mock private PartRepository partRepo;
    @Mock private OutboxService outboxService;

    private TrayService trayService;

    @BeforeEach
    void setUp()
    {
        trayService = new TrayService(trayRepo, partRepo, new TrayMapper(), outboxService);
    }

    @Test
    void createsAnOpenEmptyTray()
    {
        when(trayRepo.save(any(Tray.class))).thenAnswer(invocation -> {
            Tray tray = invocation.getArgument(0);
            tray.setId(10L);
            return tray;
        });

        var result = trayService.createTray(new CreateTrayRequest(PartType.LEG, new BigDecimal("5.00")));

        assertEquals(10L, result.id());
        assertEquals(PartType.LEG, result.type());
        assertEquals(TrayStatus.OPEN, result.status());
        assertEquals(new BigDecimal("5.00"), result.maxWeight());
        assertEquals(BigDecimal.ZERO, result.currentWeight());
        assertTrue(result.partIds().isEmpty());
        assertNull(result.sealedAt());
        verifyNoInteractions(outboxService);
    }

    @Test
    void addsMatchingPartAtExactCapacityUsingDecimalWeights()
    {
        Tray tray = tray(10L, "0.30");
        Part existing = part(1L, PartType.LEG, "0.10");
        existing.setTray(tray);
        tray.getParts().add(existing);
        Part incoming = part(2L, PartType.LEG, "0.20");
        givenLockedTray(tray);
        when(partRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(incoming));

        var result = trayService.addPart(10L, 2L);

        assertEquals(new BigDecimal("0.30"), result.currentWeight());
        assertEquals(List.of(1L, 2L), result.partIds());
        assertSame(tray, incoming.getTray());
        assertEquals(2, tray.getParts().size());
        verifyNoInteractions(outboxService);
    }

    @Test
    void rejectsPartOfADifferentType()
    {
        Tray tray = tray(10L, "5.00");
        Part part = part(1L, PartType.LOIN, "1.00");
        givenLockedTray(tray);
        when(partRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(part));

        assertThrows(PartTypeMismatchException.class, () -> trayService.addPart(10L, 1L));

        assertNull(part.getTray());
        assertTrue(tray.getParts().isEmpty());
        verifyNoInteractions(outboxService);
    }

    @Test
    void rejectsPartThatWouldExceedRemainingCapacity()
    {
        Tray tray = tray(10L, "2.00");
        Part existing = part(1L, PartType.LEG, "1.25");
        existing.setTray(tray);
        tray.getParts().add(existing);
        Part incoming = part(2L, PartType.LEG, "0.76");
        givenLockedTray(tray);
        when(partRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(incoming));

        assertThrows(TrayCapacityExceededException.class, () -> trayService.addPart(10L, 2L));

        assertNull(incoming.getTray());
        assertEquals(List.of(existing), tray.getParts());
        assertEquals(new BigDecimal("1.25"), tray.getCurrentWeight());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rejectsPartAlreadyAssignedToTheSameOrAnotherTray(boolean sameTray)
    {
        Tray tray = tray(10L, "5.00");
        Tray previousTray = sameTray ? tray : tray(20L, "5.00");
        Part part = part(1L, PartType.LEG, "1.00");
        part.setTray(previousTray);
        previousTray.getParts().add(part);
        givenLockedTray(tray);
        when(partRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(part));

        assertThrows(PartAlreadyAssignedException.class, () -> trayService.addPart(10L, 1L));

        assertSame(previousTray, part.getTray());
        assertEquals(List.of(part), previousTray.getParts());
        assertEquals(sameTray ? 1 : 0, tray.getParts().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"add", "remove"})
    void cannotChangePartsInASealedTray(String operation)
    {
        Tray tray = tray(10L, "5.00");
        tray.setStatus(TrayStatus.SEALED);
        givenLockedTray(tray);

        assertThrows(TraySealedException.class, () -> {
            if (operation.equals("add"))
            {
                trayService.addPart(10L, 1L);
            } else
            {
                trayService.removePart(10L, 1L);
            }
        });

        verifyNoInteractions(partRepo, outboxService);
    }

    @Test
    void removesPartFromBothSidesAndRecalculatesWeight()
    {
        Tray tray = tray(10L, "5.00");
        Part removed = part(1L, PartType.LEG, "1.25");
        Part retained = part(2L, PartType.LEG, "0.50");
        removed.setTray(tray);
        retained.setTray(tray);
        tray.getParts().addAll(List.of(removed, retained));
        givenLockedTray(tray);
        when(partRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(removed));

        var result = trayService.removePart(10L, 1L);

        assertNull(removed.getTray());
        assertSame(tray, retained.getTray());
        assertEquals(List.of(2L), result.partIds());
        assertEquals(new BigDecimal("0.50"), result.currentWeight());
        verifyNoInteractions(outboxService);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void rejectsRemovingAnUnassignedPartOrPartFromAnotherTray(boolean assignedElsewhere)
    {
        Tray tray = tray(10L, "5.00");
        Part part = part(1L, PartType.LEG, "1.25");
        Tray previousTray = assignedElsewhere ? tray(20L, "5.00") : null;
        part.setTray(previousTray);
        givenLockedTray(tray);
        when(partRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(part));

        assertThrows(PartNotInTrayException.class, () -> trayService.removePart(10L, 1L));

        assertSame(previousTray, part.getTray());
        assertTrue(tray.getParts().isEmpty());
    }

    @Test
    void sealsANonemptyTrayAndRecordsAnEvent()
    {
        Tray tray = tray(10L, "5.00");
        Part part = part(1L, PartType.LEG, "1.25");
        part.setTray(tray);
        tray.getParts().add(part);
        givenLockedTray(tray);
        LocalDateTime before = LocalDateTime.now();

        var result = trayService.sealTray(10L);

        assertEquals(TrayStatus.SEALED, result.status());
        assertNotNull(result.sealedAt());
        assertFalse(result.sealedAt().isBefore(before));
        assertFalse(result.sealedAt().isAfter(LocalDateTime.now()));
        assertEquals(List.of(1L), result.partIds());
        verify(outboxService).recordTraySealed(tray);
    }

    @Test
    void cannotSealAnEmptyTray()
    {
        Tray tray = tray(10L, "5.00");
        givenLockedTray(tray);

        assertThrows(TrayEmptyException.class, () -> trayService.sealTray(10L));

        assertEquals(TrayStatus.OPEN, tray.getStatus());
        assertNull(tray.getSealedAt());
        verifyNoInteractions(outboxService);
    }

    @Test
    void cannotSealATrayTwice()
    {
        Tray tray = tray(10L, "5.00");
        tray.setStatus(TrayStatus.SEALED);
        LocalDateTime sealedAt = LocalDateTime.of(2026, 11, 19, 10, 0);
        tray.setSealedAt(sealedAt);
        givenLockedTray(tray);

        assertThrows(TraySealedException.class, () -> trayService.sealTray(10L));

        assertEquals(sealedAt, tray.getSealedAt());
        verifyNoInteractions(outboxService);
    }

    @Test
    void reportsUnknownTrayWhenReading()
    {
        when(trayRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TrayNotFoundException.class, () -> trayService.getTray(99L));
    }

    @Test
    void reportsUnknownTrayBeforeChangingInventory()
    {
        when(trayRepo.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(TrayNotFoundException.class, () -> trayService.addPart(99L, 1L));

        verifyNoInteractions(partRepo, outboxService);
    }

    @Test
    void reportsUnknownPartWithoutChangingTheTray()
    {
        Tray tray = tray(10L, "5.00");
        givenLockedTray(tray);
        when(partRepo.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(PartNotFoundException.class, () -> trayService.addPart(10L, 99L));

        assertTrue(tray.getParts().isEmpty());
        verifyNoInteractions(outboxService);
    }

    private void givenLockedTray(Tray tray)
    {
        when(trayRepo.findByIdForUpdate(tray.getId())).thenReturn(Optional.of(tray));
    }

    private Tray tray(long id, String maxWeight)
    {
        Tray tray = new Tray();
        tray.setId(id);
        tray.setType(PartType.LEG);
        tray.setMaxWeight(new BigDecimal(maxWeight));
        return tray;
    }

    private Part part(long id, PartType type, String weight)
    {
        Part part = new Part();
        part.setId(id);
        part.setType(type);
        part.setWeight(new BigDecimal(weight));
        return part;
    }
}
