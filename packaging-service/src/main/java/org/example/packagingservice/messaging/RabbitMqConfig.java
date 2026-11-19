package org.example.packagingservice.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig
{
    public static final String EVENTS_EXCHANGE = "slaughterhouse.events";
    public static final String TRAY_SEALED_KEY = "tray.sealed";
    public static final String TRAY_SEALED_QUEUE = "packaging.tray-sealed";
    public static final String PRODUCT_CREATED_KEY = "product.created";

    @Bean
    public TopicExchange eventsExchange()
    {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public Queue traySealedQueue()
    {
        return new Queue(TRAY_SEALED_QUEUE, true);
    }

    @Bean
    public Binding traySealedBinding(Queue traySealedQueue, TopicExchange eventsExchange)
    {
        return BindingBuilder.bind(traySealedQueue).to(eventsExchange).with(TRAY_SEALED_KEY);
    }
}
