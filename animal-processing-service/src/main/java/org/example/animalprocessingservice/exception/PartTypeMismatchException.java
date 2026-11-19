package org.example.animalprocessingservice.exception;

import org.example.animalprocessingservice.entity.PartType;

public class PartTypeMismatchException extends RuntimeException
{
    public PartTypeMismatchException(PartType trayType, PartType partType)
    {
        super("Part type '%s' does not match tray type '%s'.".formatted(partType, trayType));
    }
}
