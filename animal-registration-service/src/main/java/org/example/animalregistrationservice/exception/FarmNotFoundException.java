package org.example.animalregistrationservice.exception;

public class FarmNotFoundException extends RuntimeException
{
    public FarmNotFoundException(Long id)
    {
        super("Farm with id=%s not found".formatted(id));
    }
}
