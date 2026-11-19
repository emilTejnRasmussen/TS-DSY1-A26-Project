package org.example.traceabilityservice.repository;

import org.example.traceabilityservice.entity.Animal;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalRepository extends JpaRepository<Animal, String>
{
}
