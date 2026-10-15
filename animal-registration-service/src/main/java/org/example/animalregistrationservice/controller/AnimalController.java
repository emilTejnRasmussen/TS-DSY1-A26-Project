package org.example.animalregistrationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.animalregistrationservice.dto.animal.AnimalDto;
import org.example.animalregistrationservice.dto.animal.RegisterAnimalDto;
import org.example.animalregistrationservice.service.AnimalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

@Controller
@RequestMapping("/animals")
@RequiredArgsConstructor
public class AnimalController
{
    private final AnimalService animalService;

    @PostMapping
    public ResponseEntity<AnimalDto> register(@Valid @RequestBody RegisterAnimalDto request)
    {
        AnimalDto animal = animalService.registerAnimal(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{registrationNumber}")
                .buildAndExpand(animal.registrationNumber())
                .toUri();

        return ResponseEntity.created(location).body(animal);
    }

    @GetMapping("/{registrationNumber}")
    public ResponseEntity<AnimalDto> getAnimal(@PathVariable String registrationNumber)
    {
        AnimalDto animal = animalService.getAnimal(registrationNumber);

        return ResponseEntity.ok(animal);
    }

    @GetMapping
    public ResponseEntity<Page<AnimalDto>> getAnimals(
            @RequestParam(required = false) LocalDate arrivalDate,
            @RequestParam(required = false) Long farmId,
            @PageableDefault(sort = "registrationNumber") Pageable pageable
    )
    {
        return ResponseEntity.ok(animalService.getAnimals(arrivalDate, farmId, pageable));
    }
}
