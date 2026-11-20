package org.example.packagingservice.exception;

public class TrayNotFoundException extends RuntimeException
{
    public TrayNotFoundException(long trayId)
    {
        super("Cannot find received tray with id = %d".formatted(trayId));
    }
}
