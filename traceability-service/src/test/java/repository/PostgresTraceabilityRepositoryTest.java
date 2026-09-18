package repository;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// I have manually put the test data into the db
class PostgresTraceabilityRepositoryTest
{
    private final TraceabilityRepository repository = new PostgresTraceabilityRepository();

    @Test
    void productOneShouldContainThreeAnimals() throws SQLException
    {
        List<String> result = repository.findAnimalRegistrationNumbersByProductId(1);

        assertEquals(List.of("A-1001", "A-1002", "A-1003"), result);
    }

    @Test
    void animalA1003ShouldBeInTwoProducts() throws SQLException
    {
        List<Integer> result = repository.findProductIdsByAnimalRegistrationNumber("A-1003");

        assertEquals(List.of(1, 2), result);
    }

    @Test
    void unknownProductShouldReturnEmptyList() throws SQLException
    {
        List<String> result = repository.findAnimalRegistrationNumbersByProductId(999);

        assertTrue(result.isEmpty());
    }

    @Test
    void unknownAnimalShouldReturnEmptyList() throws SQLException
    {
        List<Integer> result = repository.findProductIdsByAnimalRegistrationNumber("A-9999");

        assertTrue(result.isEmpty());
    }
}