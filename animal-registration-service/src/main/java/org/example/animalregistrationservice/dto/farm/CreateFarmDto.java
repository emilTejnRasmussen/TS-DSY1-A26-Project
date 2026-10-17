package org.example.animalregistrationservice.dto.farm;

import jakarta.validation.constraints.NotBlank;

public record CreateFarmDto(@NotBlank String name)
{}
