package org.example.packagingservice.mapper;

import org.example.packagingservice.dto.PartResponse;
import org.example.packagingservice.entity.Part;
import org.springframework.stereotype.Component;

@Component
public class PartMapper
{
    public PartResponse toDto(Part part)
    {
        return new PartResponse(
                part.getId(),
                part.getTray().getType(),
                part.getWeight(),
                part.getAnimalRegistrationNumber(),
                part.getTray().getId(),
                part.getProduct() != null ? part.getProduct().getId() : null
        );
    }
}
