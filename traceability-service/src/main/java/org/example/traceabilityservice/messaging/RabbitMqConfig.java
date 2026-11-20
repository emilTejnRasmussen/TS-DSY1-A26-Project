package org.example.traceabilityservice.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig
{
    public static final String EVENTS_EXCHANGE = "slaughterhouse.events";
    public static final String ANIMAL_REGISTERED_KEY = "animal.registered";
    public static final String ANIMAL_REGISTERED_QUEUE = "traceability.animal-registered";
    public static final String PART_REGISTERED_KEY = "part.registered";
    public static final String PART_REGISTERED_QUEUE = "traceability.part-registered";
    public static final String TRAY_SEALED_KEY = "tray.sealed";
    public static final String TRAY_SEALED_QUEUE = "traceability.tray-sealed";
    public static final String PRODUCT_CREATED_KEY = "product.created";
    public static final String PRODUCT_CREATED_QUEUE = "traceability.product-created";

    @Bean
    public Declarables traceabilityTopology()
    {
        TopicExchange exchange = new TopicExchange(EVENTS_EXCHANGE, true, false);
        Queue animals = new Queue(ANIMAL_REGISTERED_QUEUE, true);
        Queue parts = new Queue(PART_REGISTERED_QUEUE, true);
        Queue trays = new Queue(TRAY_SEALED_QUEUE, true);
        Queue products = new Queue(PRODUCT_CREATED_QUEUE, true);

        return new Declarables(exchange, animals, parts, trays, products,
                BindingBuilder.bind(animals).to(exchange).with(ANIMAL_REGISTERED_KEY),
                BindingBuilder.bind(parts).to(exchange).with(PART_REGISTERED_KEY),
                BindingBuilder.bind(trays).to(exchange).with(TRAY_SEALED_KEY),
                BindingBuilder.bind(products).to(exchange).with(PRODUCT_CREATED_KEY));
    }
}
