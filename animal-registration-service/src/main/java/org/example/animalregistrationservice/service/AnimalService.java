package org.example.animalregistrationservice.service;

import org.example.animalregistrationservice.dto.RegisterAnimalDto;
import org.example.animalregistrationservice.enitity.Animal;
import org.example.animalregistrationservice.enitity.Farm;
import org.example.animalregistrationservice.exception.FarmNotFoundException;
import org.example.animalregistrationservice.repository.AnimalRepository;
import org.example.animalregistrationservice.repository.FarmRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AnimalService
{
    private final FarmRepository farmRepository;
    private final AnimalRepository animalRepository;

    public AnimalService(FarmRepository farmRepository, AnimalRepository animalRepository)
    {
        this.farmRepository = farmRepository;
        this.animalRepository = animalRepository;
    }

    public void registerAnimal(RegisterAnimalDto request) {
        Farm farm = farmRepository.findById(request.farmId())
                .orElseThrow(() -> new FarmNotFoundException(request.farmId()));

        Animal animal = new Animal();
        animal.setArrivalDate(LocalDate.now());
        animal.setWeight(request.weight());
        animal.setOrigin(farm);

        animal.generateRegistrationNumber();

        animalRepository.save(animal);
    }
}
