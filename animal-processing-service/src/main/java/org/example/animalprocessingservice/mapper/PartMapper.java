package org.example.animalprocessingservice.mapper;

import org.example.animalprocessingservice.dto.PartDto;
import org.example.animalprocessingservice.entity.Part;
import org.springframework.stereotype.Component;

@Component
public class PartMapper
{

    public PartDto toDto(Part part){
        return new PartDto(
                part.getId(),
                part.getWeight(),
                part.getType(),
                part.getAnimalReference().getRegistrationNumber(),
                part.getTray().getId()
        );
    }

}
