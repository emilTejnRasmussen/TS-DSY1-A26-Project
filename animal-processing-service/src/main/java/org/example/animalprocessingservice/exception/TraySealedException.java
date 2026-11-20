package org.example.animalprocessingservice.exception;

public class TraySealedException extends RuntimeException
{
    public TraySealedException(long trayId)
    {
        super("Tray with ID " + trayId + " is already sealed.");
    }
}
