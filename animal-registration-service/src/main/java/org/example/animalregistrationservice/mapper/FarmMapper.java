package org.example.animalregistrationservice.mapper;

import org.example.animalregistrationservice.dto.farm.CreateFarmDto;
import org.example.animalregistrationservice.dto.farm.FarmDto;
import org.example.animalregistrationservice.enitity.Farm;
import org.springframework.stereotype.Component;

@Component
public class FarmMapper
{
    public Farm fromCreateRequest(CreateFarmDto request)
    {
        Farm farm = new Farm();
        farm.setName(request.name());

        return farm;
    }

    public FarmDto toDto(Farm farm)
    {
        return new FarmDto(
                farm.getId(),
                farm.getName()
        );
    }
}