package org.example.animalprocessingservice.exception;

public class TrayNotFoundException extends RuntimeException
{
    public TrayNotFoundException(Long trayId)
    {
        super("Cannot find tray with id = %d".formatted(trayId));
    }
}
