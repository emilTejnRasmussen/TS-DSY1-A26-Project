package org.example.animalregistrationservice.controller;

import org.example.animalregistrationservice.dto.animal.AnimalDto;
import org.example.animalregistrationservice.enitity.AnimalType;
import org.example.animalregistrationservice.exception.AnimalNotFoundException;
import org.example.animalregistrationservice.exception.FarmNotFoundException;
import org.example.animalregistrationservice.service.AnimalService;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(AnimalController.class)
@DisplayName("Animal Controller")
class AnimalControllerTest
{

    private static final String BASE_URL = "/api/v1/animals";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AnimalService animalService;

    private AnimalDto animal;

    @BeforeEach
    void setUp()
    {
        animal = new AnimalDto(
                "DK-2026-test123",
                AnimalType.PIG,
                LocalDate.of(2026, 10, 15),
                BigDecimal.valueOf(245.5),
                1L,
                "Sunds Farm"
        );
    }

    @Nested
    @DisplayName("Register Animal")
    class RegisterAnimalTests
    {

        @Test
        @DisplayName("Should register animal successfully")
        void shouldRegisterAnimal() throws Exception
        {
            when(animalService.registerAnimal(any()))
                    .thenReturn(animal);

            String body = """
                    {
                        "type": "PIG",
                        "weight": 245.5,
                        "farmId": 1
                    }
                    """;

            MvcResult result = mockMvc.perform(post(BASE_URL)
                            .servletPath("/api/v1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn();

            assertAll("Registered animal",
                    () -> assertEquals(201, result.getResponse().getStatus()),
                    () -> assertEquals(
                            "PIG",
                            com.jayway.jsonpath.JsonPath.read(
                                    result.getResponse().getContentAsString(),
                                    "$.type"
                            )
                    ),
                    () -> assertTrue(
                            Objects.requireNonNull(result.getResponse().getHeader("Location"))
                                    .endsWith("/animals/DK-2026-test123")
                    ),
                    () -> verify(animalService).registerAnimal(any())
            );
        }

        @Test
        @DisplayName("Should reject invalid animal data")
        void shouldRejectInvalidData() throws Exception
        {
            String body = """
                    {
                        "type": null,
                        "weight": -10,
                        "farmId": null
                    }
                    """;

            MvcResult result = mockMvc.perform(post(BASE_URL)
                            .servletPath("/api/v1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn();

            String json = result.getResponse().getContentAsString();

            assertAll("Validation errors",
                    () -> assertEquals(400, result.getResponse().getStatus()),
                    () -> assertEquals("Invalid request data",
                            com.jayway.jsonpath.JsonPath.read(json, "$.title")),
                    () -> assertEquals("Validation failed",
                            com.jayway.jsonpath.JsonPath.read(json, "$.detail")),
                    () -> assertTrue(json.contains("weight")),
                    () -> assertTrue(json.contains("farmId")),
                    () -> assertTrue(json.contains("type")),
                    () -> verifyNoInteractions(animalService)
            );
        }

        @Test
        @DisplayName("Should reject invalid animal type")
        void shouldRejectInvalidAnimalType() throws Exception
        {
            String body = """
                    {
                        "type": "DRAGON",
                        "weight": 200,
                        "farmId": 1
                    }
                    """;

            MvcResult result = mockMvc.perform(post(BASE_URL)
                            .servletPath("/api/v1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn();

            assertAll(
                    () -> assertEquals(400, result.getResponse().getStatus()),
                    () -> verifyNoInteractions(animalService)
            );
        }

        @Test
        @DisplayName("Should return 404 when farm does not exist")
        void shouldReturnNotFoundForMissingFarm() throws Exception
        {
            when(animalService.registerAnimal(any()))
                    .thenThrow(new FarmNotFoundException(999L));

            String body = """
                    {
                        "type": "PIG",
                        "weight": 200,
                        "farmId": 999
                    }
                    """;

            MvcResult result = mockMvc.perform(post(BASE_URL)
                            .servletPath("/api/v1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn();

            assertAll(
                    () -> assertEquals(404, result.getResponse().getStatus()),
                    () -> assertEquals("Not Found",
                            com.jayway.jsonpath.JsonPath.read(
                                    result.getResponse().getContentAsString(),
                                    "$.title"
                            ))
            );
        }

        @Test
        @DisplayName("Should return 500 for unexpected errors")
        void shouldHandleUnexpectedError() throws Exception
        {
            when(animalService.registerAnimal(any()))
                    .thenThrow(new RuntimeException("Database unavailable"));

            String body = """
                    {
                        "type": "PIG",
                        "weight": 200,
                        "farmId": 1
                    }
                    """;

            MvcResult result = mockMvc.perform(post(BASE_URL)
                            .servletPath("/api/v1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andReturn();

            String json = result.getResponse().getContentAsString();

            assertAll(
                    () -> assertEquals(500, result.getResponse().getStatus()),
                    () -> assertEquals("Internal server error",
                            com.jayway.jsonpath.JsonPath.read(json, "$.title")),
                    () -> assertFalse(json.contains("Database unavailable"))
            );
        }
    }

    @Nested
    @DisplayName("Get Animal")
    class GetAnimalTests
    {

        @Test
        @DisplayName("Should return existing animal")
        void shouldReturnAnimal() throws Exception
        {
            when(animalService.getAnimal("DK-2026-test123"))
                    .thenReturn(animal);

            MvcResult result = mockMvc.perform(
                            get(BASE_URL + "/DK-2026-test123")
                                    .servletPath("/api/v1"))
                    .andReturn();

            String json = result.getResponse().getContentAsString();

            assertAll(
                    () -> assertEquals(200, result.getResponse().getStatus()),
                    () -> assertEquals("DK-2026-test123",
                            com.jayway.jsonpath.JsonPath.read(json, "$.registrationNumber")),
                    () -> assertEquals("PIG",
                            com.jayway.jsonpath.JsonPath.read(json, "$.type")),
                    () -> verify(animalService).getAnimal("DK-2026-test123")
            );
        }

        @Test
        @DisplayName("Should return 404 when animal does not exist")
        void shouldReturnNotFound() throws Exception
        {
            when(animalService.getAnimal("UNKNOWN"))
                    .thenThrow(new AnimalNotFoundException("UNKNOWN"));

            MvcResult result = mockMvc.perform(
                            get(BASE_URL + "/UNKNOWN")
                                    .servletPath("/api/v1"))
                    .andReturn();

            assertAll(
                    () -> assertEquals(404, result.getResponse().getStatus()),
                    () -> assertEquals("Not Found",
                            com.jayway.jsonpath.JsonPath.read(
                                    result.getResponse().getContentAsString(),
                                    "$.title"
                            ))
            );
        }
    }

    @Nested
    @DisplayName("Get Animals")
    class GetAnimalsTests
    {

        @Test
        @DisplayName("Should return empty page")
        void shouldReturnEmptyPage() throws Exception
        {
            when(animalService.getAnimals(
                    isNull(), isNull(), any()))
                    .thenReturn(Page.empty());

            MvcResult result = mockMvc.perform(
                            get(BASE_URL)
                                    .servletPath("/api/v1"))
                    .andReturn();

            assertAll(
                    () -> assertEquals(200, result.getResponse().getStatus()),
                    () -> assertEquals(0,
                            ((Number) com.jayway.jsonpath.JsonPath.read(
                                    result.getResponse().getContentAsString(),
                                    "$.totalElements"
                            )).intValue())
            );
        }

        @Test
        @DisplayName("Should forward filters and pagination")
        void shouldForwardFiltersAndPagination() throws Exception
        {
            LocalDate date = LocalDate.of(2026, 10, 15);

            Page<AnimalDto> page = new PageImpl<>(
                    List.of(animal),
                    PageRequest.of(1, 5, Sort.by("arrivalDate").descending()),
                    10
            );

            when(animalService.getAnimals(
                    eq(date), eq(1L), any()))
                    .thenReturn(page);

            MvcResult result = mockMvc.perform(get(BASE_URL)
                            .servletPath("/api/v1")
                            .param("arrivalDate", "2026-10-15")
                            .param("farmId", "1")
                            .param("page", "1")
                            .param("size", "5")
                            .param("sort", "arrivalDate,desc"))
                    .andReturn();

            assertAll(
                    () -> assertEquals(200, result.getResponse().getStatus()),
                    () -> verify(animalService).getAnimals(
                            eq(date),
                            eq(1L),
                            argThat(p ->
                                    p.getPageNumber() == 1
                                            && p.getPageSize() == 5
                                            && p.getSort().getOrderFor("arrivalDate")
                                            .getDirection() == Sort.Direction.DESC
                            )
                    )
            );
        }
    }
}