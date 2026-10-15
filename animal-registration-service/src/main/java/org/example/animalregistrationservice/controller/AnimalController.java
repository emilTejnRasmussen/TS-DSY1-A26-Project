package org.example.animalregistrationservice.controller;

import jakarta.validation.Valid;
import org.example.animalregistrationservice.dto.RegisterAnimalDto;
import org.example.animalregistrationservice.service.AnimalService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/api/v1/animals")
public class AnimalController
{
    private AnimalService animalService;

    public AnimalController(AnimalService animalService)
    {
        this.animalService = animalService;
    }

    @PostMapping
    public void register(@Valid @RequestBody RegisterAnimalDto request){
        animalService
    }
}
