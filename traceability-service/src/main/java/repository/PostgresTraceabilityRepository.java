package repository;

import database.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PostgresTraceabilityRepository implements TraceabilityRepository
{
    @Override
    public List<String> findAnimalRegistrationNumbersByProductId(int productId) throws SQLException
    {
        String sql = """
                SELECT DISTINCT a.registration_number
                FROM animal a
                JOIN part p ON p.animal_registration_number = a.registration_number
                JOIN tray_part tp ON tp.part_id = p.part_id
                JOIN product_tray pt ON pt.tray_id = tp.tray_id
                WHERE pt.product_id = ?;
                """;

        List<String> registrationNumbers = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        )
        {
            statement.setInt(1, productId);

            try (ResultSet resultSet = statement.executeQuery())
            {
                while (resultSet.next())
                {
                    registrationNumbers.add(resultSet.getString("registration_number"));
                }
            }
        }

        return registrationNumbers;
    }

    @Override
    public List<Integer> findProductIdsByAnimalRegistrationNumber(String registrationNumber) throws SQLException
    {
        String sql = """
                SELECT DISTINCT pt.product_id
                FROM animal a
                JOIN part p ON p.animal_registration_number = a.registration_number
                JOIN tray_part tp ON tp.part_id = p.part_id
                JOIN product_tray pt ON pt.tray_id = tp.tray_id
                WHERE a.registration_number = ?;
                """;

        List<Integer> productIds = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)
        )
        {
            statement.setString(1, registrationNumber);

            try (ResultSet resultSet = statement.executeQuery())
            {
                while (resultSet.next())
                {
                    productIds.add(resultSet.getInt("product_id"));
                }
            }
        }
        return productIds;
    }
}
