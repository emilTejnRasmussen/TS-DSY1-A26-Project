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
                JOIN part p ON p.animal_id = a.animal_id
                JOIN product_tray pt ON pt.tray_id = p.tray_id
                WHERE pt.product_id = ?
                ORDER BY a.registration_number
                """;

        List<String> registrationNumbers = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
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
                SELECT DISTINCT pr.product_id
                FROM product pr
                JOIN product_tray pt ON pt.product_id = pr.product_id
                JOIN part p ON p.tray_id = pt.tray_id
                JOIN animal a ON a.animal_id = p.animal_id
                WHERE a.registration_number = ?
                ORDER BY pr.product_id
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
