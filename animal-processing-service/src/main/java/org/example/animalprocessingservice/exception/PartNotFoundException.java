package org.example.animalprocessingservice.exception;

public class AnimalNotFoundException extends RuntimeException {
  public AnimalNotFoundException(String message) {
    super(message);
  }
}
