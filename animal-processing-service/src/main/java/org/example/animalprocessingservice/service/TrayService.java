package org.example.animalprocessingservice.service;

import lombok.RequiredArgsConstructor;
import org.example.animalprocessingservice.dto.CreateTrayRequest;
import org.example.animalprocessingservice.dto.TrayResponse;
import org.example.animalprocessingservice.entity.Part;
import org.example.animalprocessingservice.entity.PartType;
import org.example.animalprocessingservice.entity.Tray;
import org.example.animalprocessingservice.entity.TrayStatus;
import org.example.animalprocessingservice.exception.*;
import org.example.animalprocessingservice.mapper.TrayMapper;
import org.example.animalprocessingservice.repository.PartRepository;
import org.example.animalprocessingservice.repository.TrayRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TrayService
{
    private final TrayRepository trayRepo;
    private final PartRepository partRepo;
    private final TrayMapper trayMapper;

    @Transactional
    public TrayResponse createTray(CreateTrayRequest request)
    {
        Tray tray = new Tray();

        tray.setType(request.type());
        tray.setMaxWeight(request.maxWeight());

        Tray savedTray = trayRepo.save(tray);

        return trayMapper.toDto(savedTray);
    }

    @Transactional(readOnly = true)
    public TrayResponse getTray(long trayId)
    {
        return trayMapper.toDto(getTrayById(trayId));
    }

    @Transactional(readOnly = true)
    public Page<TrayResponse> getTrays(
            PartType type,
            TrayStatus status,
            Pageable pageable
    )
    {
        Page<Tray> trays;

        if (type != null && status != null)
        {
            trays = trayRepo.findByTypeAndStatus(type, status, pageable);
        } else if (type != null)
        {
            trays = trayRepo.findByType(type, pageable);
        } else if (status != null)
        {
            trays = trayRepo.findByStatus(status, pageable);
        } else
        {
            trays = trayRepo.findAll(pageable);
        }

        return trays.map(trayMapper::toDto);
    }

    @Transactional
    public TrayResponse addPart(long trayId, long partId)
    {
        Tray tray = getTrayByIdForUpdate(trayId);

        ensureTrayIsOpen(tray);

        Part part = getPartByIdForUpdate(partId);

        ensurePartIsUnassigned(part);
        ensurePartTypeMatchesTray(tray.getType(), part.getType());
        ensureTrayHasCapacity(tray, part.getWeight());

        tray.getParts().add(part);
        part.setTray(tray);

        return trayMapper.toDto(tray);
    }

    @Transactional
    public TrayResponse removePart(long trayId, long partId)
    {
        Tray tray = getTrayByIdForUpdate(trayId);

        ensureTrayIsOpen(tray);

        Part part = getPartByIdForUpdate(partId);

        ensurePartBelongsToTray(part, tray);

        tray.getParts().remove(part);
        part.setTray(null);

        return trayMapper.toDto(tray);
    }

    private Tray getTrayById(long trayId){
        return trayRepo.findById(trayId)
                .orElseThrow(() -> new TrayNotFoundException(trayId));
    }

    private Tray getTrayByIdForUpdate(long trayId)
    {
        return trayRepo.findByIdForUpdate(trayId)
                .orElseThrow(() -> new TrayNotFoundException(trayId));
    }

    private Part getPartByIdForUpdate(long partId)
    {
        return partRepo.findByIdForUpdate(partId)
                .orElseThrow(() -> new PartNotFoundException(partId));
    }

    private void ensurePartIsUnassigned(Part part)
    {
        if (part.getTray() != null){
            throw new PartAlreadyAssignedException();
        }
    }

    private void ensureTrayIsOpen(Tray tray){
        if (tray.getStatus() != TrayStatus.OPEN)
        {
            throw new TraySealedException();
        }
    }

    private void ensurePartTypeMatchesTray(PartType trayType, PartType partType)
    {
        if (trayType != partType){
            throw new PartTypeMismatchException(trayType, partType);
        }
    }


    private void ensureTrayHasCapacity(Tray tray, BigDecimal partWeight)
    {
        BigDecimal newWeight = tray.getCurrentWeight().add(partWeight);

        if (newWeight.compareTo(tray.getMaxWeight()) > 0){
            throw new TrayCapacityExceededException();
        }
    }

    private void ensurePartBelongsToTray(Part part, Tray tray)
    {
        if (part.getTray() == null ||
                !Objects.equals(part.getTray().getId(), tray.getId()))
        {
            throw new PartNotInTrayException();
        }
    }
}
