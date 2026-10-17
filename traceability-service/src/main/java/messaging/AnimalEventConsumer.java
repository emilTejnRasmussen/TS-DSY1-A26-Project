package messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.*;
import events.AnimalRegisteredEvent;
import repository.TraceabilityRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

public class AnimalEventConsumer implements AutoCloseable
{
    private final TraceabilityRepository repository;

    private static final String QUEUE_NAME = "traceability.animal-registered";

    private final Connection connection;
    private final Channel channel;
    private final ObjectMapper objectMapper;

    public AnimalEventConsumer(TraceabilityRepository repository) throws IOException, TimeoutException
    {
        this.repository = repository;

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        connection = factory.newConnection();
        channel = connection.createChannel();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    public void start() throws IOException
    {
        DeliverCallback deliverCallback = (consumerTag, delivery) ->
        {
            long deliveryTag = delivery.getEnvelope().getDeliveryTag();

            try
            {
                String json = new String(
                        delivery.getBody(),
                        StandardCharsets.UTF_8
                );

                AnimalRegisteredEvent event = objectMapper.readValue(
                        json,
                        AnimalRegisteredEvent.class
                );

                System.out.println("Received animal: " + event.registrationNumber());

                repository.registerAnimal(event);
                channel.basicAck(deliveryTag, false);
            } catch (Exception e)
            {
                System.err.println("Failed to process animal event: " + e.getMessage());
                channel.basicNack(deliveryTag, false, true);
            }
        };

        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {
        });

        System.out.println("Listening for animal registration events...");
    }

    @Override
    public void close() throws Exception
    {
        channel.close();
        connection.close();
    }
}