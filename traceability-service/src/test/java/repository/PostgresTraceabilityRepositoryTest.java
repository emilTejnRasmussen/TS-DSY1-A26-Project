package repository;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// I have manually put the test data into the db
class PostgresTraceabilityRepositoryTest
{
    private final TraceabilityRepository repository = new PostgresTraceabilityRepository();

    @Test
    void productOneShouldContainTwoAnimals() throws SQLException
    {
        List<String> animals =
                repository.findAnimalRegistrationNumbersByProductId(1);

        assertEquals(
                Set.of("DK-2026-TEST001", "DK-2026-TEST002"),
                Set.copyOf(animals)
        );
    }

    @Test
    void animalTest002ShouldBeInTwoProducts() throws SQLException
    {
        List<Integer> products = repository.findProductIdsByAnimalRegistrationNumber("DK-2026-TEST002");

        assertEquals(Set.of(1, 2), Set.copyOf(products));
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