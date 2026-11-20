package org.example.packagingservice.exception;

public class InvalidProductCompositionException extends RuntimeException
{
    public InvalidProductCompositionException()
    {
        super("A product must contain at least one part, with valid and distinct part IDs.");
    }
}
