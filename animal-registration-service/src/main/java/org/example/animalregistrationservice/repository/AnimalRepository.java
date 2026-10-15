package org.example.animalregistrationservice.repository;

import org.example.animalregistrationservice.enitity.Animal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, String>
{
    Page<Animal> findByArrivalDate(
            LocalDate arrivalDate,
            Pageable pageable
    );

    Page<Animal> findByOriginId(
            Long farmId,
            Pageable pageable
    );

    Page<Animal> findByArrivalDateAndOriginId(
            LocalDate arrivalDate,
            Long farmId,
            Pageable pageable
    );
}
