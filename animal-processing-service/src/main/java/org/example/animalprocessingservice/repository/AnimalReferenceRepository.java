package org.example.animalprocessingservice.repository;

import org.example.animalprocessingservice.entity.AnimalReference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnimalReferenceRepository extends JpaRepository<AnimalReference, String>
{
}
