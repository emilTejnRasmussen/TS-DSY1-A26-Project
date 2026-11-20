package org.example.animalprocessingservice.exception;

public class PartNotInTrayException extends RuntimeException
{
    public PartNotInTrayException()
    {
        super("Part cannot be removed from tray it is not in.");
    }
}
