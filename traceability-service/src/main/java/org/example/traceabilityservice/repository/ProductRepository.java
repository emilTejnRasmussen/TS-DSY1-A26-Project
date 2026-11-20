package org.example.traceabilityservice.repository;

import jakarta.persistence.LockModeType;
import org.example.traceabilityservice.entity.Product;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>
{
    @Modifying(flushAutomatically = true)
    @Query(value = "INSERT INTO product (product_id) VALUES (:id) ON CONFLICT DO NOTHING", nativeQuery = true)
    int insertIfAbsent(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Query("SELECT p.id FROM Product p JOIN p.parts part WHERE part.id = :partId")
    Optional<Long> findProductIdByPartId(@Param("partId") Long partId);

    @Query("""
            SELECT DISTINCT part.animal.registrationNumber FROM Product p JOIN p.parts part
            WHERE p.id = :productId ORDER BY part.animal.registrationNumber
            """)
    List<String> findAnimalRegistrationNumbersByProductId(@Param("productId") Long productId);

    @Query("""
            SELECT DISTINCT p.id FROM Product p JOIN p.parts part
            WHERE part.animal.registrationNumber = :registrationNumber ORDER BY p.id
            """)
    List<Long> findProductIdsByAnimalRegistrationNumber(@Param("registrationNumber") String registrationNumber);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.id = :id AND p.parts IS EMPTY")
    boolean existsWithoutPartLinks(@Param("id") Long id);

    @Query("""
            SELECT COUNT(p) > 0 FROM Product p JOIN p.trays tray JOIN tray.parts part
            WHERE p.parts IS EMPTY AND part.animal.registrationNumber = :registrationNumber
            """)
    boolean existsLegacyProductForAnimal(@Param("registrationNumber") String registrationNumber);
}
