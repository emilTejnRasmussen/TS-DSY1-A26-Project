package org.example.animalprocessingservice.mapper;

import org.example.animalprocessingservice.dto.TrayResponse;
import org.example.animalprocessingservice.entity.Part;
import org.example.animalprocessingservice.entity.Tray;
import org.springframework.stereotype.Component;

@Component
public class TrayMapper
{
    public TrayResponse toDto(Tray tray){
        return new TrayResponse(
                tray.getId(),
                tray.getType(),
                tray.getStatus(),
                tray.getMaxWeight(),
                tray.getCurrentWeight(),
                tray.getParts()
                        .stream()
                        .map(Part::getId)
                        .toList(),
                tray.getCreatedAt(),
                tray.getSealedAt()
        );
    }
}
