package org.example.traceabilityservice.repository;

import org.example.traceabilityservice.entity.Animal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnimalRepository extends JpaRepository<Animal, String>
{
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO animal (registration_number) VALUES (:registrationNumber)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("registrationNumber") String registrationNumber);
}
