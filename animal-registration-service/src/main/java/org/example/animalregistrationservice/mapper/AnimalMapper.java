package org.example.animalregistrationservice.mapper;

import org.example.animalregistrationservice.dto.AnimalDto;
import org.example.animalregistrationservice.dto.RegisterAnimalDto;
import org.example.animalregistrationservice.enitity.Animal;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class AnimalMapper
{
    public AnimalDto toDto(Animal animal)
    {
        return new AnimalDto(
                animal.getRegistrationNumber(),
                animal.getArrivalDate(),
                animal.getWeight(),
                animal.getOrigin().getId(),
                animal.getOrigin().getName()
        );
    }

    public Animal fromRegisterRequest(RegisterAnimalDto request)
    {
        Animal animal = new Animal();
        animal.setArrivalDate(LocalDate.now());
        animal.setWeight(request.weight());

        return animal;
    }
}
