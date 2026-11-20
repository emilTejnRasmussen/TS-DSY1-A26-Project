package org.example.animalprocessingservice.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig
{
    public static final String EVENTS_EXCHANGE = "slaughterhouse.events";
    public static final String ANIMAL_REGISTERED_KEY = "animal.registered";
    public static final String ANIMAL_REGISTERED_QUEUE = "processing.animal-registered";

    public static final String PART_REGISTERED_KEY = "part.registered";
    public static final String TRAY_SEALED_KEY = "tray.sealed";

    @Bean
    public TopicExchange eventsExchange()
    {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue animalRegisteredQueue()
    {
        return new Queue(ANIMAL_REGISTERED_QUEUE, true);
    }

    @Bean
    public Binding animalRegisteredBinding(
            Queue animalRegisteredQueue,
            TopicExchange eventsExchange)
    {
        return BindingBuilder
                .bind(animalRegisteredQueue)
                .to(eventsExchange)
                .with(ANIMAL_REGISTERED_KEY);
    }
}