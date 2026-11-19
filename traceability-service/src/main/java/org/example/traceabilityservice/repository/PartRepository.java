package org.example.traceabilityservice.repository;

import jakarta.persistence.LockModeType;
import org.example.traceabilityservice.entity.Part;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PartRepository extends JpaRepository<Part, Long>
{
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO part (part_id, animal_registration_number) VALUES (:id, :registrationNumber)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") Long id, @Param("registrationNumber") String registrationNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Part p WHERE p.id = :id")
    Optional<Part> findByIdForUpdate(@Param("id") Long id);
}
