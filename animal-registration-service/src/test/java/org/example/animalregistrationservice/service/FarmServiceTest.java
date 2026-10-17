package org.example.animalregistrationservice.service;

import org.example.animalregistrationservice.dto.farm.CreateFarmDto;
import org.example.animalregistrationservice.dto.farm.FarmDto;
import org.example.animalregistrationservice.enitity.Farm;
import org.example.animalregistrationservice.exception.FarmNotFoundException;
import org.example.animalregistrationservice.mapper.FarmMapper;
import org.example.animalregistrationservice.repository.FarmRepository;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Farm Service")
class FarmServiceTest
{

    @Mock
    private FarmRepository farmRepository;

    private FarmService farmService;

    @BeforeEach
    void setUp()
    {
        farmService = new FarmService(
                farmRepository,
                new FarmMapper()
        );
    }

    private Farm createFarm(Long id, String name)
    {
        Farm farm = new Farm();
        farm.setId(id);
        farm.setName(name);
        return farm;
    }

    @Nested
    @DisplayName("Create Farm")
    class CreateFarmTests
    {

        @Test
        @DisplayName("Should create farm successfully")
        void shouldCreateFarm()
        {
            CreateFarmDto request = new CreateFarmDto("Sunds Farm");

            when(farmRepository.save(any(Farm.class)))
                    .thenAnswer(invocation -> {
                        Farm farm = invocation.getArgument(0);
                        farm.setId(1L);
                        return farm;
                    });

            FarmDto result = farmService.createFarm(request);

            assertAll("Created farm",
                    () -> assertEquals(1L, result.id()),
                    () -> assertEquals("Sunds Farm", result.name()),
                    () -> verify(farmRepository).save(any(Farm.class))
            );
        }
    }

    @Nested
    @DisplayName("Get Farm")
    class GetFarmTests
    {

        @Test
        @DisplayName("Should return existing farm")
        void shouldReturnFarm()
        {
            Farm farm = createFarm(1L, "Sunds Farm");

            when(farmRepository.findById(1L))
                    .thenReturn(Optional.of(farm));

            FarmDto result = farmService.getFarm(1L);

            assertAll("Retrieved farm",
                    () -> assertEquals(1L, result.id()),
                    () -> assertEquals("Sunds Farm", result.name()),
                    () -> verify(farmRepository).findById(1L)
            );
        }

        @Test
        @DisplayName("Should throw exception when farm does not exist")
        void shouldThrowWhenFarmDoesNotExist()
        {
            when(farmRepository.findById(999L))
                    .thenReturn(Optional.empty());

            assertAll(
                    () -> assertThrows(
                            FarmNotFoundException.class,
                            () -> farmService.getFarm(999L)
                    ),
                    () -> verify(farmRepository).findById(999L)
            );
        }
    }

    @Nested
    @DisplayName("Get Farms")
    class GetFarmsTests
    {

        private Pageable pageable;

        @BeforeEach
        void setUp()
        {
            pageable = PageRequest.of(0, 10);
        }

        @Test
        @DisplayName("Should return empty page when no farms exist")
        void shouldReturnEmptyPage()
        {
            when(farmRepository.findAll(pageable))
                    .thenReturn(Page.empty(pageable));

            Page<FarmDto> result = farmService.getFarms(
                    null, pageable
            );

            assertAll("Empty page",
                    () -> assertTrue(result.isEmpty()),
                    () -> assertEquals(0, result.getTotalElements()),
                    () -> verify(farmRepository).findAll(pageable)
            );
        }

        @Test
        @DisplayName("Should return one farm")
        void shouldReturnOneFarm()
        {
            Farm farm = createFarm(1L, "Sunds Farm");

            when(farmRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(List.of(farm), pageable, 1));

            Page<FarmDto> result = farmService.getFarms(
                    null, pageable
            );

            assertAll(
                    () -> assertEquals(1, result.getTotalElements()),
                    () -> assertEquals("Sunds Farm", result.getContent().getFirst().name()),
                    () -> verify(farmRepository).findAll(pageable)
            );
        }

        @Test
        @DisplayName("Should return multiple farms")
        void shouldReturnMultipleFarms()
        {
            List<Farm> farms = List.of(
                    createFarm(1L, "Sunds Farm"),
                    createFarm(2L, "Herning Farm"),
                    createFarm(3L, "Aarhus Farm")
            );

            when(farmRepository.findAll(pageable))
                    .thenReturn(new PageImpl<>(farms, pageable, 3));

            Page<FarmDto> result = farmService.getFarms(
                    null, pageable
            );

            assertAll("Multiple farms",
                    () -> assertEquals(3, result.getTotalElements()),
                    () -> assertEquals(3, result.getContent().size()),
                    () -> assertEquals("Sunds Farm", result.getContent().get(0).name()),
                    () -> assertEquals("Herning Farm", result.getContent().get(1).name()),
                    () -> assertEquals("Aarhus Farm", result.getContent().get(2).name())
            );
        }

        @Test
        @DisplayName("Should search farms by name")
        void shouldSearchByName()
        {
            Farm farm = createFarm(1L, "Sunds Farm");

            when(farmRepository.findByNameContainingIgnoreCase("sund", pageable))
                    .thenReturn(new PageImpl<>(List.of(farm), pageable, 1));

            Page<FarmDto> result = farmService.getFarms(
                    "sund", pageable
            );

            assertAll("Search farms",
                    () -> assertEquals(1, result.getTotalElements()),
                    () -> assertEquals("Sunds Farm", result.getContent().getFirst().name()),
                    () -> verify(farmRepository)
                            .findByNameContainingIgnoreCase("sund", pageable)
            );
        }

        @Test
        @DisplayName("Should return empty page when no names match")
        void shouldReturnNoMatches()
        {
            when(farmRepository.findByNameContainingIgnoreCase("unknown", pageable))
                    .thenReturn(Page.empty(pageable));

            Page<FarmDto> result = farmService.getFarms(
                    "unknown", pageable
            );

            assertAll(
                    () -> assertTrue(result.isEmpty()),
                    () -> assertEquals(0, result.getTotalElements()),
                    () -> verify(farmRepository)
                            .findByNameContainingIgnoreCase("unknown", pageable)
            );
        }

        @Test
        @DisplayName("Should return all farms when search is blank")
        void shouldHandleBlankSearch()
        {
            when(farmRepository.findAll(pageable))
                    .thenReturn(Page.empty(pageable));

            Page<FarmDto> result = farmService.getFarms(
                    "   ", pageable
            );

            assertAll(
                    () -> assertTrue(result.isEmpty()),
                    () -> verify(farmRepository).findAll(pageable),
                    () -> verify(farmRepository, never())
                            .findByNameContainingIgnoreCase(any(), any())
            );
        }

        @Test
        @DisplayName("Should trim search term")
        void shouldTrimSearch()
        {
            when(farmRepository.findByNameContainingIgnoreCase("sund", pageable))
                    .thenReturn(Page.empty(pageable));

            Page<FarmDto> result = farmService.getFarms(
                    "  sund  ", pageable
            );

            assertAll(
                    () -> assertTrue(result.isEmpty()),
                    () -> verify(farmRepository)
                            .findByNameContainingIgnoreCase("sund", pageable)
            );
        }

        @Test
        @DisplayName("Should preserve pagination information")
        void shouldPreservePagination()
        {
            Pageable secondPage = PageRequest.of(1, 2);

            List<Farm> farms = List.of(
                    createFarm(3L, "Farm C"),
                    createFarm(4L, "Farm D")
            );

            when(farmRepository.findAll(secondPage))
                    .thenReturn(new PageImpl<>(farms, secondPage, 5));

            Page<FarmDto> result = farmService.getFarms(
                    null, secondPage
            );

            assertAll("Pagination",
                    () -> assertEquals(1, result.getNumber()),
                    () -> assertEquals(2, result.getSize()),
                    () -> assertEquals(5, result.getTotalElements()),
                    () -> assertEquals(3, result.getTotalPages()),
                    () -> assertEquals(2, result.getContent().size()),
                    () -> assertFalse(result.isFirst()),
                    () -> assertFalse(result.isLast()),
                    () -> verify(farmRepository).findAll(secondPage)
            );
        }
    }
}