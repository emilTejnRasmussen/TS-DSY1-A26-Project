package org.example.animalregistrationservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.animalregistrationservice.dto.AnimalDto;
import org.example.animalregistrationservice.dto.RegisterAnimalDto;
import org.example.animalregistrationservice.service.AnimalService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@Controller
@RequestMapping("/animals")
@RequiredArgsConstructor
public class AnimalController
{
    private final AnimalService animalService;

    @PostMapping
    public ResponseEntity<AnimalDto> register(@Valid @RequestBody RegisterAnimalDto request){
        AnimalDto animal = animalService.registerAnimal(request);

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{registrationNumber}")
                .buildAndExpand(animal.registrationNumber())
                .toUri();

        return ResponseEntity.created(location).body(animal);
    }
}
