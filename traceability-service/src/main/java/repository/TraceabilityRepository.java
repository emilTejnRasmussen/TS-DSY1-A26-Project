package repository;

import java.sql.SQLException;
import java.util.List;

public interface TraceabilityRepository {

    List<String> findAnimalRegistrationNumbersByProductId(int productId) throws SQLException;

    List<Integer> findProductIdsByAnimalRegistrationNumber(String registrationNumber) throws SQLException;
}