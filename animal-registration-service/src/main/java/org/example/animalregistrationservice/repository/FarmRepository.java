package org.example.animalregistrationservice.repository;

import org.example.animalregistrationservice.enitity.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FarmRepository extends JpaRepository<Farm, Long>
{
}