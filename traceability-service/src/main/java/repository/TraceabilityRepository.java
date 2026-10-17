package repository;

import events.AnimalRegisteredEvent;

import java.sql.SQLException;
import java.util.List;

public interface TraceabilityRepository {

    List<String> findAnimalRegistrationNumbersByProductId(int productId) throws SQLException;

    List<Integer> findProductIdsByAnimalRegistrationNumber(String registrationNumber) throws SQLException;

    void registerAnimal(AnimalRegisteredEvent event) throws SQLException;
}