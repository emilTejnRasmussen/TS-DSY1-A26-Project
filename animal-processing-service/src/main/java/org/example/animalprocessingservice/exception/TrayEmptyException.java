package org.example.animalprocessingservice.exception;

public class TrayEmptyException extends RuntimeException
{
    public TrayEmptyException(long id)
    {
        super("Cannot seal empty tray.");
    }
}
