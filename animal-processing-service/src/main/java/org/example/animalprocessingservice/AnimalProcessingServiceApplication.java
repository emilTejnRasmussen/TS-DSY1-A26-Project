package org.example.animalprocessingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class AnimalProcessingServiceApplication
{

    public static void main(String[] args)
    {
        SpringApplication.run(AnimalProcessingServiceApplication.class, args);
    }

}
