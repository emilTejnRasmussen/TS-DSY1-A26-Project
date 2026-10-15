package org.example.animalregistrationservice.repository;

import org.example.animalregistrationservice.enitity.Animal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AnimalRepository extends JpaRepository<Animal, String>
{
    List<Animal> findByArrivalDate(LocalDate arrivalDate);

    List<Animal> findByOriginId(Long farmId);
}
