package org.example.packagingservice.service;

import lombok.RequiredArgsConstructor;
import org.example.packagingservice.dto.TrayResponse;
import org.example.packagingservice.entity.PartType;
import org.example.packagingservice.entity.Tray;
import org.example.packagingservice.exception.TrayNotFoundException;
import org.example.packagingservice.mapper.TrayMapper;
import org.example.packagingservice.repository.TrayRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrayService
{
    private final TrayRepository trayRepo;
    private final TrayMapper trayMapper;

    @Transactional(readOnly = true)
    public TrayResponse getTray(long trayId)
    {
        Tray tray = trayRepo.findById(trayId)
                .orElseThrow(() -> new TrayNotFoundException(trayId));

        return trayMapper.toDto(tray);
    }

    @Transactional(readOnly = true)
    public Page<TrayResponse> getTrays(PartType type, boolean availableOnly, Pageable pageable)
    {
        return trayRepo.findTrays(type, availableOnly, pageable).map(trayMapper::toDto);
    }
}
