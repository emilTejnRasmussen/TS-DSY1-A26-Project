package org.example.animalregistrationservice.service;

import jakarta.transaction.Transactional;
import org.example.animalregistrationservice.dto.AnimalDto;
import org.example.animalregistrationservice.dto.RegisterAnimalDto;
import org.example.animalregistrationservice.enitity.Animal;
import org.example.animalregistrationservice.enitity.Farm;
import org.example.animalregistrationservice.exception.FarmNotFoundException;
import org.example.animalregistrationservice.mapper.AnimalMapper;
import org.example.animalregistrationservice.repository.AnimalRepository;
import org.example.animalregistrationservice.repository.FarmRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AnimalService
{
    private final FarmRepository farmRepository;
    private final AnimalRepository animalRepository;
    private final AnimalMapper animalMapper;

    public AnimalService(FarmRepository farmRepository, AnimalRepository animalRepository, AnimalMapper animalMapper)
    {
        this.farmRepository = farmRepository;
        this.animalRepository = animalRepository;
        this.animalMapper = animalMapper;
    }

    @Transactional
    public AnimalDto registerAnimal(RegisterAnimalDto request) {
        Farm farm = farmRepository.findById(request.farmId())
                .orElseThrow(() -> new FarmNotFoundException(request.farmId()));

        Animal animal = animalMapper.fromRegisterRequest(request);

        animal.setOrigin(farm);
        animal.generateRegistrationNumber();

        Animal savedAnimal = animalRepository.save(animal);

        return animalMapper.toDto(savedAnimal);
    }
}
