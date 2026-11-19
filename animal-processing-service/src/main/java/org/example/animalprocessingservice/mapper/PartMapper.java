package org.example.animalprocessingservice.mapper;

import org.example.animalprocessingservice.dto.PartResponse;
import org.example.animalprocessingservice.entity.Part;
import org.springframework.stereotype.Component;

@Component
public class PartMapper
{

    public PartResponse toDto(Part part){
        Long trayId = part.getTray() != null
                ? part.getTray().getId()
                : null;

        return new PartResponse(
                part.getId(),
                part.getWeight(),
                part.getType(),
                part.getAnimalReference().getRegistrationNumber(),
                trayId
        );
    }

}
