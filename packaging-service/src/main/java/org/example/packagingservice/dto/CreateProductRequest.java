package org.example.packagingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateProductRequest(
        @NotBlank
        @Size(max = 255)
        String name,

        @NotEmpty
        List<@NotNull @Positive Long> partIds
)
{
}
