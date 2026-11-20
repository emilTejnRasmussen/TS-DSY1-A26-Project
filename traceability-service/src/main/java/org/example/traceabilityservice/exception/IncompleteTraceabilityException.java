package org.example.traceabilityservice.exception;

public class IncompleteTraceabilityException extends RuntimeException
{
    public IncompleteTraceabilityException(String message)
    {
        super(message);
    }
}
