package org.example.packagingservice.messaging;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.packagingservice.entity.Part;
import org.example.packagingservice.entity.Tray;
import org.example.packagingservice.repository.TrayRepository;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrayEventConsumer
{
    private final TrayRepository trayRepo;
    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMqConfig.TRAY_SEALED_QUEUE)
    @Transactional
    public void consume(String message)
    {
        TraySealedEvent event = objectMapper.readValue(message, TraySealedEvent.class);
        validateEvent(event);

        if (trayRepo.existsById(event.trayId()) || trayRepo.existsByEventId(event.eventId()))
        {
            log.info("Tray already received: {}", event.trayId());
            return;
        }

        Tray tray = new Tray();
        tray.setId(event.trayId());
        tray.setEventId(event.eventId());
        tray.setType(event.partType());
        tray.setTotalWeight(event.totalWeight());
        tray.setSealedAt(event.occurredAt());

        for (TraySealedEvent.TrayPart trayPart : event.parts())
        {
            Part part = new Part();
            part.setId(trayPart.partId());
            part.setAnimalRegistrationNumber(trayPart.animalRegistrationNumber());
            part.setWeight(trayPart.weight());
            part.setTray(tray);
            tray.getParts().add(part);
        }

        // External IDs must be inserted, never merged into existing inventory.
        entityManager.persist(tray);

        log.info("Received sealed tray: {}", event.trayId());
    }

    private void validateEvent(TraySealedEvent event)
    {
        if (event == null || event.eventId() == null || !"TraySealed".equals(event.eventType())
                || event.occurredAt() == null || event.trayId() == null || event.trayId() <= 0
                || event.partType() == null || !validWeight(event.totalWeight())
                || event.parts() == null || event.parts().isEmpty())
        {
            throw new IllegalArgumentException("Missing or invalid tray information");
        }

        Set<Long> partIds = new HashSet<>();
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (TraySealedEvent.TrayPart part : event.parts())
        {
            if (part == null || part.partId() == null || part.partId() <= 0
                    || !partIds.add(part.partId()) || !validWeight(part.weight())
                    || part.animalRegistrationNumber() == null
                    || part.animalRegistrationNumber().isBlank()
                    || part.animalRegistrationNumber().length() > 255)
            {
                throw new IllegalArgumentException("Missing or invalid part information");
            }

            totalWeight = totalWeight.add(part.weight());
        }

        if (totalWeight.compareTo(event.totalWeight()) != 0)
        {
            throw new IllegalArgumentException("Tray weight does not match its parts");
        }
    }

    private boolean validWeight(BigDecimal weight)
    {
        return weight != null && weight.signum() > 0
                && weight.stripTrailingZeros().scale() <= 2
                && weight.compareTo(new BigDecimal("99999999.99")) <= 0;
    }
}
