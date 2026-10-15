package org.example.animalregistrationservice.mapper;

import org.example.animalregistrationservice.dto.animal.AnimalDto;
import org.example.animalregistrationservice.dto.animal.RegisterAnimalDto;
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
                animal.getType(),
                animal.getArrivalDate(),
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
        animal.setArrivalDate(LocalDate.now());

        return animal;
    }
}
