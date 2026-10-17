package messaging;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;

import java.io.IOException;

public final class RabbitMqTopology
{
    public static final String EVENTS_EXCHANGE = "slaughterhouse.events";
    public static final String ANIMAL_REGISTERED_KEY = "animal.registered";
    public static final String ANIMAL_REGISTERED_QUEUE = "traceability.animal-registered";

    private RabbitMqTopology()
    {
    }

    public static void declare(Channel channel) throws IOException
    {
        channel.exchangeDeclare(
                EVENTS_EXCHANGE,
                BuiltinExchangeType.TOPIC,
                true
        );

        channel.queueDeclare(
                ANIMAL_REGISTERED_QUEUE,
                true,
                false,
                false,
                null
        );

        channel.queueBind(
                ANIMAL_REGISTERED_QUEUE,
                EVENTS_EXCHANGE,
                ANIMAL_REGISTERED_KEY
        );
    }
}