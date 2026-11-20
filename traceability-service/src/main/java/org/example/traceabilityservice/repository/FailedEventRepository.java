package org.example.traceabilityservice.repository;

import org.example.traceabilityservice.entity.FailedEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FailedEventRepository extends JpaRepository<FailedEvent, UUID>
{
}
