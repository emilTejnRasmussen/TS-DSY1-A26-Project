package org.example.animalprocessingservice.exception;

public class TrayCapacityExceededException extends RuntimeException
{
    public TrayCapacityExceededException()
    {
        super("Cannot exceed max weight of tray");
    }
}
