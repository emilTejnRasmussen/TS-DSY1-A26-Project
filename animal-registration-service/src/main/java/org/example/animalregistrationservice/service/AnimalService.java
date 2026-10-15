package org.example.animalregistrationservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.example.animalregistrationservice.dto.animal.AnimalDto;
import org.example.animalregistrationservice.dto.animal.RegisterAnimalDto;
import org.example.animalregistrationservice.enitity.Animal;
import org.example.animalregistrationservice.enitity.Farm;
import org.example.animalregistrationservice.exception.AnimalNotFoundException;
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
    public AnimalDto registerAnimal(RegisterAnimalDto request)
    {
        Farm farm = farmRepository.findById(request.farmId())
                .orElseThrow(() -> new FarmNotFoundException(request.farmId()));

        Animal animal = animalMapper.fromRegisterRequest(request);

        animal.setOrigin(farm);
        animal.generateRegistrationNumber();

        Animal savedAnimal = animalRepository.save(animal);

        return animalMapper.toDto(savedAnimal);
    }

    @Transactional(readOnly = true)
    public AnimalDto getAnimal(String registrationNumber)
    {
        Animal animal = animalRepository.findById(registrationNumber)
                .orElseThrow(() -> new AnimalNotFoundException(registrationNumber));

        return animalMapper.toDto(animal);
    }

    @Transactional(readOnly = true)
    public Page<AnimalDto> getAnimals(LocalDate arrivalDate, Long farmId, Pageable pageable)
    {
        Page<Animal> animals;

        if (arrivalDate != null && farmId != null)
        {
            animals = animalRepository.findByArrivalDateAndOriginId(arrivalDate, farmId, pageable);

        } else if (arrivalDate != null)
        {
            animals = animalRepository.findByArrivalDate(arrivalDate, pageable);

        } else if (farmId != null)
        {
            animals = animalRepository.findByOriginId(farmId, pageable);

        } else
        {
            animals = animalRepository.findAll(pageable);
        }

        return animals.map(animalMapper::toDto);
    }
}
