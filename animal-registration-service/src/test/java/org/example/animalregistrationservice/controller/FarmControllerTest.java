package org.example.animalregistrationservice.controller;

import org.example.animalregistrationservice.dto.farm.FarmDto;
import org.example.animalregistrationservice.exception.FarmNotFoundException;
import org.example.animalregistrationservice.service.FarmService;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

@WebMvcTest(FarmController.class)
@DisplayName("Farm Controller")
class FarmControllerTest
{

    private static final String BASE_URL = "/api/v1/farms";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FarmService farmService;

    private FarmDto farm;

    @BeforeEach
    void setUp()
    {
        farm = new FarmDto(1L, "Sunds Farm");
    }

    @Nested
    @DisplayName("Register Farm")
    class RegisterFarmTests
    {

        @Test
        @DisplayName("Should register farm successfully")
        void shouldRegisterFarm() throws Exception
        {
            when(farmService.createFarm(any()))
                    .thenReturn(farm);

            MvcResult result = mockMvc.perform(post(BASE_URL)
                            .servletPath("/api/v1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "Sunds Farm"
                                    }
                                    """))
                    .andReturn();

            assertAll(
                    () -> assertEquals(201, result.getResponse().getStatus()),
                    () -> assertTrue(
                            Objects.requireNonNull(result.getResponse().getHeader("Location"))
                                    .endsWith("/farms/1")
                    ),
                    () -> assertEquals("Sunds Farm",
                            com.jayway.jsonpath.JsonPath.read(
                                    result.getResponse().getContentAsString(),
                                    "$.name"
                            )),
                    () -> verify(farmService).createFarm(any())
            );
        }

        @Test
        @DisplayName("Should reject blank farm name")
        void shouldRejectBlankName() throws Exception
        {
            MvcResult result = mockMvc.perform(post(BASE_URL)
                            .servletPath("/api/v1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "name": "   "
                                    }
                                    """))
                    .andReturn();

            String json = result.getResponse().getContentAsString();

            assertAll(
                    () -> assertEquals(400, result.getResponse().getStatus()),
                    () -> assertEquals("Validation failed",
                            com.jayway.jsonpath.JsonPath.read(json, "$.detail")),
                    () -> assertTrue(json.contains("name")),
                    () -> verifyNoInteractions(farmService)
            );
        }

        @Test
        @DisplayName("Should reject missing farm name")
        void shouldRejectMissingName() throws Exception
        {
            MvcResult result = mockMvc.perform(post(BASE_URL)
                            .servletPath("/api/v1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andReturn();

            assertAll(
                    () -> assertEquals(400, result.getResponse().getStatus()),
                    () -> verifyNoInteractions(farmService)
            );
        }
    }

    @Nested
    @DisplayName("Get Farm")
    class GetFarmTests
    {

        @Test
        @DisplayName("Should return existing farm")
        void shouldReturnFarm() throws Exception
        {
            when(farmService.getFarm(1L))
                    .thenReturn(farm);

            MvcResult result = mockMvc.perform(
                            get(BASE_URL + "/1")
                                    .servletPath("/api/v1"))
                    .andReturn();

            String json = result.getResponse().getContentAsString();

            assertAll(
                    () -> assertEquals(200, result.getResponse().getStatus()),
                    () -> assertEquals("Sunds Farm",
                            com.jayway.jsonpath.JsonPath.read(json, "$.name")),
                    () -> verify(farmService).getFarm(1L)
            );
        }

        @Test
        @DisplayName("Should return 404 when farm does not exist")
        void shouldReturnNotFound() throws Exception
        {
            when(farmService.getFarm(999L))
                    .thenThrow(new FarmNotFoundException(999L));

            MvcResult result = mockMvc.perform(
                            get(BASE_URL + "/999")
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
    @DisplayName("Get Farms")
    class GetFarmsTests
    {

        @Test
        @DisplayName("Should return empty page")
        void shouldReturnEmptyPage() throws Exception
        {
            when(farmService.getFarms(isNull(), any()))
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
        @DisplayName("Should return multiple farms")
        void shouldReturnMultipleFarms() throws Exception
        {
            Page<FarmDto> page = new PageImpl<>(
                    List.of(
                            new FarmDto(1L, "Sunds Farm"),
                            new FarmDto(2L, "Herning Farm")
                    ),
                    PageRequest.of(0, 10),
                    2
            );

            when(farmService.getFarms(isNull(), any()))
                    .thenReturn(page);

            MvcResult result = mockMvc.perform(
                            get(BASE_URL)
                                    .servletPath("/api/v1"))
                    .andReturn();

            String json = result.getResponse().getContentAsString();

            assertAll(
                    () -> assertEquals(200, result.getResponse().getStatus()),
                    () -> assertEquals(2,
                            ((Number) com.jayway.jsonpath.JsonPath.read(
                                    json, "$.totalElements"
                            )).intValue()),
                    () -> assertEquals(2,
                            ((List<?>) com.jayway.jsonpath.JsonPath.read(
                                    json, "$.content"
                            )).size())
            );
        }

        @Test
        @DisplayName("Should forward name filter and pagination")
        void shouldForwardFilters() throws Exception
        {
            when(farmService.getFarms(eq("sund"), any()))
                    .thenReturn(Page.empty());

            MvcResult result = mockMvc.perform(get(BASE_URL)
                            .servletPath("/api/v1")
                            .param("nameContains", "sund")
                            .param("page", "1")
                            .param("size", "5")
                            .param("sort", "name,desc"))
                    .andReturn();

            assertAll(
                    () -> assertEquals(200, result.getResponse().getStatus()),
                    () -> verify(farmService).getFarms(
                            eq("sund"),
                            argThat(p ->
                                    p.getPageNumber() == 1
                                            && p.getPageSize() == 5
                                            && !p.getSort().isUnsorted()
                            )
                    )
            );
        }

        @Test
        @DisplayName("Should return 400 for invalid farm ID")
        void shouldRejectInvalidFarmId() throws Exception
        {
            MvcResult result = mockMvc.perform(
                            get(BASE_URL + "/abc")
                                    .servletPath("/api/v1"))
                    .andReturn();

            assertAll(
                    () -> assertEquals(400, result.getResponse().getStatus()),
                    () -> verifyNoInteractions(farmService)
            );
        }
    }
}