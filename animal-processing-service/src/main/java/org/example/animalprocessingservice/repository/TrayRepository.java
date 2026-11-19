package org.example.animalprocessingservice.repository;

import org.example.animalprocessingservice.entity.Tray;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
public interface TrayRepository extends JpaRepository<Tray, Long>
{
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Tray t WHERE t.id = :id")
    Optional<Tray> findByIdForUpdate(@Param("id") Long id);
}
