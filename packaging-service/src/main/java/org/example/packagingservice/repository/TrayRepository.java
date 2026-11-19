package org.example.packagingservice.repository;

import org.example.packagingservice.entity.PartType;
import org.example.packagingservice.entity.Tray;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface TrayRepository extends JpaRepository<Tray, Long>
{
    boolean existsByEventId(UUID eventId);

    @Query("""
            SELECT t FROM Tray t
            WHERE (:type IS NULL OR t.type = :type)
            AND (:availableOnly = false OR EXISTS (
                SELECT p.id FROM Part p WHERE p.tray = t AND p.product IS NULL
            ))
            """)
    Page<Tray> findTrays(@Param("type") PartType type,
                         @Param("availableOnly") boolean availableOnly,
                         Pageable pageable);
}
