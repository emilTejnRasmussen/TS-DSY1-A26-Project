package org.example.animalregistrationservice.repository;

import org.example.animalregistrationservice.enitity.Farm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmRepository extends JpaRepository<Farm, Long> {

    Page<Farm> findByNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );
}