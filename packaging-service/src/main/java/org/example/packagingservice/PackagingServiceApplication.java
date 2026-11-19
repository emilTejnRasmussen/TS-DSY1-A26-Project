package org.example.packagingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PackagingServiceApplication
{

    public static void main(String[] args)
    {
        SpringApplication.run(PackagingServiceApplication.class, args);
    }

}
