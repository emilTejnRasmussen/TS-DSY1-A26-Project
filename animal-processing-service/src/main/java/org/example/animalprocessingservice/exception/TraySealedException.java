package org.example.animalprocessingservice.exception;

public class TraySealedException extends RuntimeException
{
    public TraySealedException()
    {
        super("Cannot add part to a sealed tray.");
    }
}
