package org.example.packagingservice.exception;

public class ProductNotFoundException extends RuntimeException
{
    public ProductNotFoundException(long productId)
    {
        super("Cannot find product with id = %d".formatted(productId));
    }
}
