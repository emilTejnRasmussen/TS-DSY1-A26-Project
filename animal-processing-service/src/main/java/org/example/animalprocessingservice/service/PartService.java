package org.example.animalprocessingservice.service;

import lombok.RequiredArgsConstructor;
import org.example.animalprocessingservice.dto.PartResponse;
import org.example.animalprocessingservice.dto.RegisterPartRequest;
import org.example.animalprocessingservice.entity.AnimalReference;
import org.example.animalprocessingservice.entity.Part;
import org.example.animalprocessingservice.entity.PartType;
import org.example.animalprocessingservice.exception.AnimalReferencenotFoundException;
import org.example.animalprocessingservice.exception.PartNotFoundException;
import org.example.animalprocessingservice.mapper.PartMapper;
import org.example.animalprocessingservice.messaging.OutboxService;
import org.example.animalprocessingservice.repository.AnimalReferenceRepository;
import org.example.animalprocessingservice.repository.PartRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PartService
{
    private final PartRepository partRepo;
    private final AnimalReferenceRepository animalReferenceRepo;

    private final OutboxService outboxService;

    private final PartMapper partMapper;

    @Transactional
    public PartResponse registerPart(RegisterPartRequest request)
    {
        AnimalReference animalReference = animalReferenceRepo.findById(request.animalRegistrationNumber())
                .orElseThrow(() -> new AnimalReferencenotFoundException(request.animalRegistrationNumber()));

        Part part = new Part();

        part.setAnimalReference(animalReference);
        part.setType(request.type());
        part.setWeight(request.weight());

        Part savedPart = partRepo.save(part);

        outboxService.recordPartRegistered(savedPart);

        return partMapper.toDto(savedPart);
    }

    @Transactional(readOnly = true)
    public PartResponse getPart(Long id)
    {
        Part part = partRepo.findById(id)
                .orElseThrow(() -> new PartNotFoundException(id));

        return partMapper.toDto(part);
    }

    @Transactional(readOnly = true)
    public Page<PartResponse> getParts(
            String animalRegistrationNumber,
            PartType type,
            Pageable pageable)
    {
        Page<Part> parts;

        if (animalRegistrationNumber != null && animalRegistrationNumber.isBlank()){
            animalRegistrationNumber = null;
        }

        if (animalRegistrationNumber != null && type != null)
        {
            parts = partRepo.findByAnimalReferenceRegistrationNumberAndType(
                    animalRegistrationNumber,
                    type,
                    pageable
            );
        } else if (animalRegistrationNumber != null)
        {
            parts = partRepo.findByAnimalReferenceRegistrationNumber(
                    animalRegistrationNumber,
                    pageable
            );
        } else if (type != null)
        {
            parts = partRepo.findByType(type, pageable);
        } else
        {
            parts = partRepo.findAll(pageable);
        }

        return parts.map(partMapper::toDto);
    }
}
