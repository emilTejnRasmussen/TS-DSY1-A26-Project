package org.example.traceabilityservice.repository;

import jakarta.persistence.LockModeType;
import org.example.traceabilityservice.entity.Tray;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TrayRepository extends JpaRepository<Tray, Long>
{
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tray t WHERE t.id = :id")
    Optional<Tray> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT t.id FROM Tray t JOIN t.parts p WHERE p.id = :partId")
    Optional<Long> findTrayIdByPartId(@Param("partId") Long partId);
}
