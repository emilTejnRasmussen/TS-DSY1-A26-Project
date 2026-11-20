package org.example.packagingservice.repository;

import org.example.packagingservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID>
{
    List<OutboxEvent> findByPublishedFalseOrderByOccurredAtAsc();
}
