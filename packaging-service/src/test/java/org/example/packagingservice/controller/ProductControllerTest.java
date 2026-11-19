package org.example.packagingservice.controller;

import org.example.packagingservice.exception.PartAlreadyConsumedException;
import org.example.packagingservice.exception.ProductNotFoundException;
import org.example.packagingservice.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest
{
    @Autowired private MockMvc mockMvc;
    @MockitoBean private ProductService productService;

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"name\":\"Pack\",\"partIds\":[]}",
            "{\"name\":\" \",\"partIds\":[1]}",
            "{\"name\":\"Pack\",\"partIds\":[null]}",
            "{\"name\":\"Pack\",\"partIds\":[0]}",
            "{\"name\":\"Pack\",\"partIds\":[-1]}"
    })
    void rejectsInvalidInput(String body) throws Exception
    {
        mockMvc.perform(post("/api/v1/products").servletPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        verifyNoInteractions(productService);
    }

    @Test
    void returnsNotFoundProblem() throws Exception
    {
        when(productService.getProduct(99L)).thenThrow(new ProductNotFoundException(99L));

        mockMvc.perform(get("/api/v1/products/99").servletPath("/api/v1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void returnsConflictForConsumedPart() throws Exception
    {
        when(productService.createProduct(any())).thenThrow(new PartAlreadyConsumedException(1L));

        mockMvc.perform(post("/api/v1/products").servletPath("/api/v1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Pack\",\"partIds\":[1]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
