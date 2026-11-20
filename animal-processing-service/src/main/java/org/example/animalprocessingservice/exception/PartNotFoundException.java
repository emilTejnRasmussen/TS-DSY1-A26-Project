package org.example.animalprocessingservice.exception;

public class PartNotFoundException extends RuntimeException
{
    public PartNotFoundException(Long partId)
    {
        super("Cannot find animal part with id = %d".formatted(partId));
    }
}
