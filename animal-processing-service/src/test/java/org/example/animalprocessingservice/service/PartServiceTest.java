package org.example.animalprocessingservice.service;

import org.example.animalprocessingservice.dto.RegisterPartRequest;
import org.example.animalprocessingservice.entity.AnimalReference;
import org.example.animalprocessingservice.entity.Part;
import org.example.animalprocessingservice.entity.PartType;
import org.example.animalprocessingservice.entity.Tray;
import org.example.animalprocessingservice.exception.AnimalReferencenotFoundException;
import org.example.animalprocessingservice.exception.PartNotFoundException;
import org.example.animalprocessingservice.mapper.PartMapper;
import org.example.animalprocessingservice.messaging.OutboxService;
import org.example.animalprocessingservice.repository.AnimalReferenceRepository;
import org.example.animalprocessingservice.repository.PartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PartServiceTest
{
    @Mock private PartRepository partRepo;
    @Mock private AnimalReferenceRepository animalReferenceRepo;
    @Mock private OutboxService outboxService;

    private PartService partService;

    @BeforeEach
    void setUp()
    {
        partService = new PartService(partRepo, animalReferenceRepo, outboxService, new PartMapper());
    }

    @Test
    void registersPartWithItsAnimalReferenceAndRecordsAnEvent()
    {
        AnimalReference animal = animal();
        when(animalReferenceRepo.findById("ANIMAL-A")).thenReturn(Optional.of(animal));
        when(partRepo.save(any(Part.class))).thenAnswer(invocation -> {
            Part part = invocation.getArgument(0);
            part.setId(101L);
            return part;
        });

        var result = partService.registerPart(
                new RegisterPartRequest("ANIMAL-A", PartType.LEG, new BigDecimal("1.25")));

        assertEquals(101L, result.id());
        assertEquals("ANIMAL-A", result.animalRegistrationNumber());
        assertEquals(PartType.LEG, result.type());
        assertEquals(new BigDecimal("1.25"), result.weight());
        assertNull(result.trayId());
        var captor = org.mockito.ArgumentCaptor.forClass(Part.class);
        verify(outboxService).recordPartRegistered(captor.capture());
        assertEquals(result.id(), captor.getValue().getId());
        assertSame(animal, captor.getValue().getAnimalReference());
    }

    @Test
    void rejectsUnknownAnimalWithoutSavingPartOrEvent()
    {
        when(animalReferenceRepo.findById("UNKNOWN")).thenReturn(Optional.empty());

        assertThrows(AnimalReferencenotFoundException.class, () -> partService.registerPart(
                new RegisterPartRequest("UNKNOWN", PartType.LEG, new BigDecimal("1.25"))));

        verifyNoInteractions(partRepo, outboxService);
    }

    @Test
    void retrievesPartWithItsAnimalAndTrayIdentifiers()
    {
        Part part = part();
        Tray tray = new Tray();
        tray.setId(10L);
        part.setTray(tray);
        when(partRepo.findById(101L)).thenReturn(Optional.of(part));

        var result = partService.getPart(101L);

        assertEquals(101L, result.id());
        assertEquals("ANIMAL-A", result.animalRegistrationNumber());
        assertEquals(10L, result.trayId());
        assertEquals(new BigDecimal("1.25"), result.weight());
    }

    @Test
    void reportsUnknownPart()
    {
        when(partRepo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(PartNotFoundException.class, () -> partService.getPart(99L));
    }

    @Test
    void filtersByAnimalAndTypeWhilePreservingPagination()
    {
        PageRequest pageable = PageRequest.of(1, 2);
        when(partRepo.findByAnimalReferenceRegistrationNumberAndType("ANIMAL-A", PartType.LEG, pageable))
                .thenReturn(new PageImpl<>(List.of(part()), pageable, 3));

        var result = partService.getParts("ANIMAL-A", PartType.LEG, pageable);

        assertEquals(1, result.getNumber());
        assertEquals(3, result.getTotalElements());
        assertEquals(101L, result.getContent().get(0).id());
        assertEquals("ANIMAL-A", result.getContent().get(0).animalRegistrationNumber());
        assertEquals(PartType.LEG, result.getContent().get(0).type());
    }

    private AnimalReference animal()
    {
        AnimalReference animal = new AnimalReference();
        animal.setRegistrationNumber("ANIMAL-A");
        return animal;
    }

    private Part part()
    {
        Part part = new Part();
        part.setId(101L);
        part.setAnimalReference(animal());
        part.setType(PartType.LEG);
        part.setWeight(new BigDecimal("1.25"));
        return part;
    }
}
