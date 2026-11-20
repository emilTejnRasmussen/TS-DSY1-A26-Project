package org.example.animalprocessingservice.repository;

import jakarta.persistence.LockModeType;
import org.example.animalprocessingservice.entity.PartType;
import org.example.animalprocessingservice.entity.Tray;
import org.example.animalprocessingservice.entity.TrayStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TrayRepository extends JpaRepository<Tray, Long>
{
    Page<Tray> findByType(
            PartType type,
            Pageable pageable);

    Page<Tray> findByStatus(
            TrayStatus status,
            Pageable pageable);

    Page<Tray> findByTypeAndStatus(
            PartType type,
            TrayStatus status,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tray t WHERE t.id = :id")
    Optional<Tray> findByIdForUpdate(@Param("id") Long id);
}
