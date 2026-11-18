package org.example.animalprocessingservice.repository;

import org.example.animalprocessingservice.entity.Tray;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrayRepository extends JpaRepository<Tray, Long>
{
}
