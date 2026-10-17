package org.example.animalregistrationservice.service;

import org.example.animalregistrationservice.dto.farm.CreateFarmDto;
import org.example.animalregistrationservice.dto.farm.FarmDto;
import org.example.animalregistrationservice.enitity.Farm;
import org.example.animalregistrationservice.exception.FarmNotFoundException;
import org.example.animalregistrationservice.mapper.FarmMapper;
import org.example.animalregistrationservice.repository.FarmRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FarmService {

    private final FarmRepository farmRepository;
    private final FarmMapper farmMapper;

    public FarmService(FarmRepository farmRepository, FarmMapper farmMapper) {
        this.farmRepository = farmRepository;
        this.farmMapper = farmMapper;
    }

    @Transactional
    public FarmDto createFarm(CreateFarmDto request) {
        Farm farm = farmMapper.fromCreateRequest(request);

        Farm savedFarm = farmRepository.save(farm);

        return farmMapper.toDto(savedFarm);
    }

    @Transactional(readOnly = true)
    public FarmDto getFarm(Long id) {
        Farm farm = farmRepository.findById(id)
                .orElseThrow(() -> new FarmNotFoundException(id));

        return farmMapper.toDto(farm);
    }

    @Transactional(readOnly = true)
    public Page<FarmDto> getFarms(String nameContains, Pageable pageable) {
        Page<Farm> farms;

        if (nameContains == null || nameContains.isBlank()) {
            farms = farmRepository.findAll(pageable);
        } else {
            farms = farmRepository.findByNameContainingIgnoreCase(
                    nameContains.trim(), pageable
            );
        }

        return farms.map(farmMapper::toDto);
    }
}