package org.example.packagingservice.exception;

public class PartNotFoundException extends RuntimeException
{
    public PartNotFoundException(long partId)
    {
        super("Cannot find received part with id = %d".formatted(partId));
    }
}
