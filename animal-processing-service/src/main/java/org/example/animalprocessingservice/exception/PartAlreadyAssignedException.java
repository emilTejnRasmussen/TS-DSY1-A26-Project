package org.example.animalprocessingservice.exception;

public class PartAlreadyAssignedException extends RuntimeException
{
    public PartAlreadyAssignedException()
    {
        super("Part is already assigned to a tray.");
    }
}
