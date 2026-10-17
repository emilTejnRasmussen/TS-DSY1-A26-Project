package repository;

import database.DatabaseConnection;
import events.AnimalRegisteredEvent;

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


    @Override
    public void registerAnimal(AnimalRegisteredEvent event) throws SQLException
    {
        String insertEventSql = """
                INSERT INTO processed_event (event_id)
                VALUES (?)
                ON CONFLICT (event_id) DO NOTHING
                """;

        String insertAnimalSql = """
                INSERT INTO animal (registration_number)
                VALUES (?)
                ON CONFLICT (registration_number) DO NOTHING
                """;

        try (Connection connection = DatabaseConnection.getConnection())
        {
            connection.setAutoCommit(false);

            try
            {
                try (PreparedStatement statement = connection.prepareStatement(insertEventSql))
                {
                    statement.setObject(1, event.eventId());

                    int insertedRows = statement.executeUpdate();

                    if (insertedRows == 0)
                    {
                        connection.rollback();
                        System.out.println("Event already processed: " + event.eventId());
                        return;
                    }
                }

                try (PreparedStatement statement = connection.prepareStatement(insertAnimalSql))
                {
                    statement.setString(1, event.registrationNumber());
                    statement.executeUpdate();
                }

                connection.commit();

                System.out.println("Registered animal: " + event.registrationNumber());
            } catch (SQLException e)
            {
                connection.rollback();
                throw e;
            }
        }
    }

}
