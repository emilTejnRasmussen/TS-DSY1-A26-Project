package org.example.animalprocessingservice.controller;

import org.example.animalprocessingservice.dto.PartResponse;
import org.example.animalprocessingservice.dto.TrayResponse;
import org.example.animalprocessingservice.entity.PartType;
import org.example.animalprocessingservice.entity.TrayStatus;
import org.example.animalprocessingservice.exception.TrayEmptyException;
import org.example.animalprocessingservice.exception.TrayNotFoundException;
import org.example.animalprocessingservice.service.PartService;
import org.example.animalprocessingservice.service.TrayService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {PartController.class, TrayController.class}, properties = "spring.config.import=")
class ProcessingControllerTest
{
    @Autowired private MockMvc mockMvc;
    @MockitoBean private PartService partService;
    @MockitoBean private TrayService trayService;

    @Test
    void registersPartWithCreatedStatusAndOrigin() throws Exception
    {
        when(partService.registerPart(any())).thenReturn(new PartResponse(
                1L, new BigDecimal("1.25"), PartType.LEG, "ANIMAL-A", null));

        mockMvc.perform(post("/api/v1/parts").servletPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"animalRegistrationNumber":"ANIMAL-A","type":"LEG","weight":1.25}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.animalRegistrationNumber").value("ANIMAL-A"));
    }

    @Test
    void createsAnOpenTray() throws Exception
    {
        when(trayService.createTray(any())).thenReturn(new TrayResponse(
                1L, PartType.LEG, TrayStatus.OPEN, new BigDecimal("2.50"),
                BigDecimal.ZERO, List.of(), null, null));

        mockMvc.perform(post("/api/v1/trays").servletPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"LEG\",\"maxWeight\":2.50}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.maxWeight").value(2.5));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"animalRegistrationNumber\":\" \",\"type\":\"LEG\",\"weight\":1.25}",
            "{\"animalRegistrationNumber\":\"ANIMAL-A\",\"type\":\"LEG\",\"weight\":0}"
    })
    void rejectsInvalidPartRequests(String body) throws Exception
    {
        mockMvc.perform(post("/api/v1/parts").servletPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(partService);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"type\":\"LEG\",\"maxWeight\":0}",
            "{\"type\":\"LEG\",\"maxWeight\":1.234}"
    })
    void rejectsInvalidTrayRequests(String body) throws Exception
    {
        mockMvc.perform(post("/api/v1/trays").servletPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(trayService);
    }

    @Test
    void missingTrayReturnsNotFoundProblem() throws Exception
    {
        when(trayService.getTray(99L)).thenThrow(new TrayNotFoundException(99L));

        mockMvc.perform(get("/api/v1/trays/99").servletPath("/api/v1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void sealingEmptyTrayReturnsConflictProblem() throws Exception
    {
        when(trayService.sealTray(1L)).thenThrow(new TrayEmptyException(1L));

        mockMvc.perform(put("/api/v1/trays/1/seal").servletPath("/api/v1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void passesFiltersAndPaginationToPartService() throws Exception
    {
        when(partService.getParts(eq("ANIMAL-A"), eq(PartType.LEG), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        mockMvc.perform(get("/api/v1/parts").servletPath("/api/v1")
                        .param("animalRegistrationNumber", "ANIMAL-A").param("partType", "LEG")
                        .param("page", "1").param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(partService).getParts(eq("ANIMAL-A"), eq(PartType.LEG), pageable.capture());
        assertEquals(1, pageable.getValue().getPageNumber());
        assertEquals(5, pageable.getValue().getPageSize());
        assertEquals(Sort.by(Sort.Direction.DESC, "registeredAt"), pageable.getValue().getSort());
    }

    @Test
    void invalidPartTypeReturnsBadRequest() throws Exception
    {
        mockMvc.perform(get("/api/v1/parts").servletPath("/api/v1").param("partType", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        verifyNoInteractions(partService);
    }
}
