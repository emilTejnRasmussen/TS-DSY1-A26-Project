package org.example.animalregistrationservice.repository;

import org.example.animalregistrationservice.enitity.Animal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, String>
{
    Page<Animal> findByRegisteredAtGreaterThanEqualAndRegisteredAtLessThan(
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );

    Page<Animal> findByOriginId(
            Long farmId,
            Pageable pageable
    );

    Page<Animal> findByRegisteredAtGreaterThanEqualAndRegisteredAtLessThanAndOriginId(
            LocalDateTime start,
            LocalDateTime end,
            Long farmId,
            Pageable pageable
    );
}