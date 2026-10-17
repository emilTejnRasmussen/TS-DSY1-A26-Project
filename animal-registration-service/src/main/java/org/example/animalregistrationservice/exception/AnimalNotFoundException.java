package org.example.animalregistrationservice.exception;

public class AnimalNotFoundException extends RuntimeException
{
    public AnimalNotFoundException(String registrationNumber)
    {
        super("Cannot find animal with registration number = %s".formatted(registrationNumber));
    }
}
