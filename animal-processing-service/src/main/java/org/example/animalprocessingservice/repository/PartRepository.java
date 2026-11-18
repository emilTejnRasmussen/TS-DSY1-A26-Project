package org.example.animalprocessingservice.repository;

import org.example.animalprocessingservice.entity.Part;
import org.example.animalprocessingservice.entity.PartType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartRepository extends JpaRepository<Part, Long>
{
    Page<Part> findByAnimalReferenceRegistrationNumber(
            String registrationNumber,
            Pageable pageable);

    Page<Part> findByType(
            PartType type,
            Pageable pageable);

    Page<Part> findByAnimalReferenceRegistrationNumberAndType(
            String registrationNumber,
            PartType type,
            Pageable pageable);
}
