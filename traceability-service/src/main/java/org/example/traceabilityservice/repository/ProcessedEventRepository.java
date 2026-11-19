package org.example.traceabilityservice.repository;

import org.example.traceabilityservice.entity.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, UUID>
{
    @Modifying(flushAutomatically = true)
    @Query(value = """
            INSERT INTO processed_event (event_id, processed_at) VALUES (:eventId, CURRENT_TIMESTAMP)
            ON CONFLICT DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("eventId") UUID eventId);
}
