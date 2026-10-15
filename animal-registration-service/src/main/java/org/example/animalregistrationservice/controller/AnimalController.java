
package org.example.animalregistrationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.animalregistrationservice.dto.animal.AnimalDto;
import org.example.animalregistrationservice.dto.animal.RegisterAnimalDto;
import org.example.animalregistrationservice.service.AnimalService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/animals")
@RequiredArgsConstructor
public class AnimalController
{
    private final AnimalService animalService;

    @PostMapping
    public ResponseEntity<AnimalDto> register(
            @Valid @RequestBody RegisterAnimalDto request)
    {
        AnimalDto animal = animalService.registerAnimal(request);

        log.info(
                "Registered animal: registrationNumber={}, type={}, farmId={}",
                animal.registrationNumber(),
                animal.type(),
                animal.farmId()
        );

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{registrationNumber}")
                .buildAndExpand(animal.registrationNumber())
                .toUri();

        return ResponseEntity.created(location).body(animal);
    }

    @GetMapping("/{registrationNumber}")
    public ResponseEntity<AnimalDto> getAnimal(
            @PathVariable String registrationNumber)
    {
        AnimalDto animal = animalService.getAnimal(registrationNumber);

        return ResponseEntity.ok(animal);
    }

    @GetMapping
    public Page<AnimalDto> getAnimals(
            @RequestParam(required = false) LocalDate registeredDate,
            @RequestParam(required = false) Long farmId,
            @PageableDefault(
                    size = 20,
                    sort = "registeredAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable)
    {
        return animalService.getAnimals(registeredDate, farmId, pageable);
    }
}
