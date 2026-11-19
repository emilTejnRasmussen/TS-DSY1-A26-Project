package org.example.packagingservice.exception;

public class PartAlreadyConsumedException extends RuntimeException
{
    public PartAlreadyConsumedException(long partId)
    {
        super("Part with ID " + partId + " is already used in a product.");
    }
}
