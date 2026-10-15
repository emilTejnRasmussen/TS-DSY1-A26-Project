package org.example.animalregistrationservice.service;

import org.example.animalregistrationservice.dto.animal.AnimalDto;
import org.example.animalregistrationservice.dto.animal.RegisterAnimalDto;
import org.example.animalregistrationservice.enitity.*;
import org.example.animalregistrationservice.exception.AnimalNotFoundException;
import org.example.animalregistrationservice.exception.FarmNotFoundException;
import org.example.animalregistrationservice.mapper.AnimalMapper;
import org.example.animalregistrationservice.repository.AnimalRepository;
import org.example.animalregistrationservice.repository.FarmRepository;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Animal Service")
class AnimalServiceTest
{

    @Mock
    private FarmRepository farmRepository;

    @Mock
    private AnimalRepository animalRepository;

    private AnimalService animalService;
    private Farm farm;

    @BeforeEach
    void setUp()
    {
        animalService = new AnimalService(
                farmRepository,
                animalRepository,
                new AnimalMapper()
        );

        farm = new Farm();
        farm.setId(1L);
        farm.setName("Sunds Farm");
    }

    private Animal createAnimal(AnimalType type, double weight)
    {
        Animal animal = new Animal();
        animal.generateRegistrationNumber();
        animal.setType(type);
        animal.setWeight(BigDecimal.valueOf(weight));
        animal.setArrivalDate(LocalDate.now());
        animal.setOrigin(farm);
        return animal;
    }

    @Nested
    @DisplayName("Register Animal")
    class RegisterAnimalTests
    {

        @Test
        @DisplayName("Should register animal successfully")
        void shouldRegisterAnimal()
        {
            RegisterAnimalDto request = new RegisterAnimalDto(
                    AnimalType.PIG,
                    BigDecimal.valueOf(245.5),
                    1L
            );

            when(farmRepository.findById(1L))
                    .thenReturn(Optional.of(farm));

            when(animalRepository.save(any(Animal.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            AnimalDto result = animalService.registerAnimal(request);

            assertAll("Registered animal",
                    () -> assertNotNull(result.registrationNumber()),
                    () -> assertTrue(result.registrationNumber().startsWith("DK-")),
                    () -> assertEquals(AnimalType.PIG, result.type()),
                    () -> assertEquals(BigDecimal.valueOf(245.5), result.weight()),
                    () -> assertEquals(1L, result.farmId()),
                    () -> assertEquals("Sunds Farm", result.farmName()),
                    () -> assertEquals(LocalDate.now(), result.arrivalDate()),
                    () -> verify(animalRepository).save(any(Animal.class))
            );
        }

        @Test
        @DisplayName("Should generate unique registration numbers")
        void shouldGenerateUniqueRegistrationNumbers()
        {
            RegisterAnimalDto request = new RegisterAnimalDto(
                    AnimalType.PIG,
                    BigDecimal.valueOf(200),
                    1L
            );

            when(farmRepository.findById(1L))
                    .thenReturn(Optional.of(farm));

            when(animalRepository.save(any(Animal.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            AnimalDto first = animalService.registerAnimal(request);
            AnimalDto second = animalService.registerAnimal(request);

            assertAll("Unique registrations",
                    () -> assertNotEquals(
                            first.registrationNumber(),
                            second.registrationNumber()
                    ),
                    () -> verify(animalRepository, times(2))
                            .save(any(Animal.class))
            );
        }

        @Test
        @DisplayName("Should throw exception when farm does not exist")
        void shouldThrowWhenFarmDoesNotExist()
        {
            RegisterAnimalDto request = new RegisterAnimalDto(
                    AnimalType.PIG,
                    BigDecimal.valueOf(245.5),
                    999L
            );

            when(farmRepository.findById(999L))
                    .thenReturn(Optional.empty());

            assertAll(
                    () -> assertThrows(
                            FarmNotFoundException.class,
                            () -> animalService.registerAnimal(request)
                    ),
                    () -> verify(animalRepository, never()).save(any())
            );
        }
    }

    @Nested
    @DisplayName("Get Animal")
    class GetAnimalTests
    {

        @Test
        @DisplayName("Should return existing animal")
        void shouldReturnAnimal()
        {
            Animal animal = createAnimal(AnimalType.COW, 500);
            String registrationNumber = animal.getRegistrationNumber();

            when(animalRepository.findById(registrationNumber))
                    .thenReturn(Optional.of(animal));

            AnimalDto result = animalService.getAnimal(registrationNumber);

            assertAll("Retrieved animal",
                    () -> assertEquals(registrationNumber, result.registrationNumber()),
                    () -> assertEquals(AnimalType.COW, result.type()),
                    () -> assertEquals(BigDecimal.valueOf(500.0), result.weight()),
                    () -> assertEquals(1L, result.farmId()),
                    () -> verify(animalRepository).findById(registrationNumber)
            );
        }

        @Test
        @DisplayName("Should throw exception when animal does not exist")
        void shouldThrowWhenAnimalDoesNotExist()
        {
            when(animalRepository.findById("UNKNOWN"))
                    .thenReturn(Optional.empty());

            assertAll(
                    () -> assertThrows(
                            AnimalNotFoundException.class,
                            () -> animalService.getAnimal("UNKNOWN")
                    ),
                    () -> verify(animalRepository).findById("UNKNOWN")
            );
        }
    }

    @Nested
    @DisplayName("Get Animals")
    class GetAnimalsTests
    {

        private Pageable pageable;

        @BeforeEach
        void setUp()
        {
            pageable = PageRequest.of(0, 10);
        }

        @Test
        @DisplayName("Should return empty page when no animals exist")
        void shouldReturnEmptyPage()
        {
            when(animalRepository.findAll(pageable))
                    .thenReturn(Page.empty(pageable));

            Page<AnimalDto> result = animalService.getAnimals(
                    null, null, pageable
            );

            assertAll("Empty page",
                    () -> assertTrue(result.isEmpty()),
                    () -> assertEquals(0, result.getTotalElements()),
                    () -> assertEquals(0, result.getTotalPages()),
                    () -> verify(animalRepository).findAll(pageable)
            );
        }

        @Test
        @DisplayName("Should return one animal")
        void shouldReturnOneAnimal()
        {
            Animal animal = createAnimal(AnimalType.PIG, 200);

            when(animalRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(animal), pageable, 1));

            Page<AnimalDto> result = animalService.getAnimals(
                    null, null, pageable
            );

            assertAll("Single animal",
                    () -> assertEquals(1, result.getTotalElements()),
                    () -> assertEquals(1, result.getContent().size()),
                    () -> assertEquals(AnimalType.PIG, result.getContent().getFirst().type())
            );
        }

        @Test
        @DisplayName("Should return multiple animals")
        void shouldReturnMultipleAnimals()
        {
            List<Animal> animals = List.of(
                    createAnimal(AnimalType.PIG, 200),
                    createAnimal(AnimalType.COW, 500),
                    createAnimal(AnimalType.CHICKEN, 3)
            );

            when(animalRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(animals, pageable, 3));

            Page<AnimalDto> result = animalService.getAnimals(
                    null, null, pageable
            );

            assertAll("Multiple animals",
                    () -> assertEquals(3, result.getTotalElements()),
                    () -> assertEquals(3, result.getContent().size()),
                    () -> assertEquals(AnimalType.PIG, result.getContent().get(0).type()),
                    () -> assertEquals(AnimalType.COW, result.getContent().get(1).type()),
                    () -> assertEquals(AnimalType.CHICKEN, result.getContent().get(2).type())
            );
        }

        @Test
        @DisplayName("Should filter by arrival date")
        void shouldFilterByArrivalDate()
        {
            LocalDate date = LocalDate.now();
            Animal animal = createAnimal(AnimalType.PIG, 200);

            when(animalRepository.findByArrivalDate(date, pageable))
                    .thenReturn(new PageImpl<>(List.of(animal), pageable, 1));

            Page<AnimalDto> result = animalService.getAnimals(
                    date, null, pageable
            );

            assertAll(
                    () -> assertEquals(1, result.getTotalElements()),
                    () -> assertEquals(date, result.getContent().getFirst().arrivalDate()),
                    () -> verify(animalRepository).findByArrivalDate(date, pageable)
            );
        }

        @Test
        @DisplayName("Should filter by farm")
        void shouldFilterByFarm()
        {
            Animal animal = createAnimal(AnimalType.PIG, 200);

            when(animalRepository.findByOriginId(1L, pageable))
                    .thenReturn(new PageImpl<>(List.of(animal), pageable, 1));

            Page<AnimalDto> result = animalService.getAnimals(
                    null, 1L, pageable
            );

            assertAll(
                    () -> assertEquals(1, result.getTotalElements()),
                    () -> assertEquals(1L, result.getContent().getFirst().farmId()),
                    () -> verify(animalRepository).findByOriginId(1L, pageable)
            );
        }

        @Test
        @DisplayName("Should filter by date and farm")
        void shouldFilterByDateAndFarm()
        {
            LocalDate date = LocalDate.now();
            Animal animal = createAnimal(AnimalType.PIG, 200);

            when(animalRepository.findByArrivalDateAndOriginId(date, 1L, pageable))
                    .thenReturn(new PageImpl<>(List.of(animal), pageable, 1));

            Page<AnimalDto> result = animalService.getAnimals(
                    date, 1L, pageable
            );

            assertAll(
                    () -> assertEquals(1, result.getTotalElements()),
                    () -> assertEquals(date, result.getContent().getFirst().arrivalDate()),
                    () -> assertEquals(1L, result.getContent().getFirst().farmId()),
                    () -> verify(animalRepository)
                            .findByArrivalDateAndOriginId(date, 1L, pageable)
            );
        }

        @Test
        @DisplayName("Should preserve pagination information")
        void shouldPreservePagination()
        {
            Pageable secondPage = PageRequest.of(1, 2);

            List<Animal> animals = List.of(
                    createAnimal(AnimalType.PIG, 200),
                    createAnimal(AnimalType.COW, 500)
            );

            when(animalRepository.findAll(secondPage))
                    .thenReturn(new PageImpl<>(animals, secondPage, 5));

            Page<AnimalDto> result = animalService.getAnimals(
                    null, null, secondPage
            );

            assertAll("Pagination",
                    () -> assertEquals(1, result.getNumber()),
                    () -> assertEquals(2, result.getSize()),
                    () -> assertEquals(5, result.getTotalElements()),
                    () -> assertEquals(3, result.getTotalPages()),
                    () -> assertEquals(2, result.getContent().size()),
                    () -> assertFalse(result.isFirst()),
                    () -> assertFalse(result.isLast()),
                    () -> verify(animalRepository).findAll(secondPage)
            );
        }
    }
}