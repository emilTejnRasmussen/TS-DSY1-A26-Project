package org.example.animalprocessingservice.repository;

import jakarta.persistence.LockModeType;
import org.example.animalprocessingservice.entity.Part;
import org.example.animalprocessingservice.entity.PartType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Part p WHERE p.id = :id")
    Optional<Part> findByIdForUpdate(@Param("id") Long id);
}
