package org.example.animalprocessingservice.exception;

public class AnimalReferencenotFoundException extends RuntimeException
{
    public AnimalReferencenotFoundException(String animalReferenceId)
    {
        super("Cannot find animal reference with registration number = %s".formatted(animalReferenceId));
    }
}
