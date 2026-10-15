package org.example.animalregistrationservice.mapper;

import org.example.animalregistrationservice.dto.animal.AnimalDto;
import org.example.animalregistrationservice.dto.animal.RegisterAnimalDto;
import org.example.animalregistrationservice.enitity.Animal;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class AnimalMapper
{
    public AnimalDto toDto(Animal animal)
    {
        return new AnimalDto(
                animal.getRegistrationNumber(),
                animal.getType(),
                animal.getRegisteredAt(),
                animal.getWeight(),
                animal.getOrigin().getId(),
                animal.getOrigin().getName()
        );
    }

    public Animal fromRegisterRequest(RegisterAnimalDto request)
    {
        Animal animal = new Animal();

        animal.setType(request.type());
        animal.setWeight(request.weight());
        animal.setRegisteredAt(LocalDateTime.now());

        return animal;
    }
}
