package org.example.packagingservice.mapper;

import lombok.RequiredArgsConstructor;
import org.example.packagingservice.dto.TrayResponse;
import org.example.packagingservice.entity.Tray;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrayMapper
{
    private final PartMapper partMapper;

    public TrayResponse toDto(Tray tray)
    {
        return new TrayResponse(
                tray.getId(),
                tray.getType(),
                tray.getTotalWeight(),
                tray.getAvailableWeight(),
                tray.getParts().stream().map(partMapper::toDto).toList(),
                tray.getSealedAt(),
                tray.getReceivedAt()
        );
    }
}
