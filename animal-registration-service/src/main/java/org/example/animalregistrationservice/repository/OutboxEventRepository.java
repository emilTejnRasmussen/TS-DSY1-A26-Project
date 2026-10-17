package org.example.animalregistrationservice.repository;

import org.example.animalregistrationservice.enitity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID>
{

}
