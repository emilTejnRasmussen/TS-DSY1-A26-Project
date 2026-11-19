package org.example.traceabilityservice.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.example.traceabilityservice.entity.*;
import org.example.traceabilityservice.messaging.*;
import org.example.traceabilityservice.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class EventProcessingService
{
    private final AnimalRepository animalRepo;
    private final PartRepository partRepo;
    private final TrayRepository trayRepo;
    private final ProductRepository productRepo;
    private final ProcessedEventRepository processedEventRepo;
    private final EntityManager entityManager;

    public void registerAnimal(AnimalRegisteredEvent event)
    {
        require(event != null, "Missing event");
        validateHeader(event.eventId(), event.eventType(), event.occurredAt(), "AnimalRegistered");
        validateRegistrationNumber(event.registrationNumber());

        if (!beginEvent(event.eventId()))
        {
            return;
        }

        getAnimal(event.registrationNumber());
    }

    public void registerPart(PartRegisteredEvent event)
    {
        require(event != null, "Missing event");
        validateHeader(event.eventId(), event.eventType(), event.occurredAt(), "PartRegistered");
        validatePart(event.partId(), event.animalRegistrationNumber(), event.weight());
        require(event.partType() != null, "Missing part type");

        if (!beginEvent(event.eventId()))
        {
            return;
        }

        Animal animal = getAnimal(event.animalRegistrationNumber());
        getPart(event.partId(), animal, event.partType(), event.weight());
    }

    public void sealTray(TraySealedEvent event)
    {
        require(event != null, "Missing event");
        validateHeader(event.eventId(), event.eventType(), event.occurredAt(), "TraySealed");
        validateId(event.trayId());
        require(event.partType() != null, "Missing part type");
        validateWeight(event.totalWeight());
        require(event.parts() != null && !event.parts().isEmpty(), "Missing tray parts");
        Set<Long> partIds = new HashSet<>();
        BigDecimal total = BigDecimal.ZERO;

        for (TraySealedEvent.TrayPart part : event.parts())
        {
            require(part != null, "Missing tray part");
            validatePart(part.partId(), part.animalRegistrationNumber(), part.weight());
            require(partIds.add(part.partId()), "Duplicate tray part ID");
            total = total.add(part.weight());
        }

        require(total.compareTo(event.totalWeight()) == 0, "Tray weight does not match its parts");
        if (!beginEvent(event.eventId()))
        {
            return;
        }

        event.parts().stream().map(TraySealedEvent.TrayPart::animalRegistrationNumber)
                .distinct().sorted().forEach(this::getAnimal);
        Tray tray = getTray(event.trayId());
        require(tray.getParts().stream().allMatch(part -> partIds.contains(part.getId())),
                "Sealed tray omits an existing part");

        for (TraySealedEvent.TrayPart source : event.parts().stream()
                .sorted(Comparator.comparing(TraySealedEvent.TrayPart::partId)).toList())
        {
            Part part = getPart(source.partId(), getAnimal(source.animalRegistrationNumber()),
                    event.partType(), source.weight());
            attachPartToTray(tray, part);
        }
    }

    public void createProduct(ProductCreatedEvent event)
    {
        require(event != null, "Missing event");
        validateHeader(event.eventId(), event.eventType(), event.occurredAt(), "ProductCreated");
        validateId(event.productId());
        require(event.parts() != null && !event.parts().isEmpty(), "Missing product parts");
        Set<Long> partIds = new HashSet<>();

        for (ProductCreatedEvent.ProductPart part : event.parts())
        {
            require(part != null, "Missing product part");
            validatePart(part.partId(), part.animalRegistrationNumber(), part.weight());
            validateId(part.trayId());
            require(partIds.add(part.partId()), "Duplicate product part ID");
        }

        if (!beginEvent(event.eventId()))
        {
            return;
        }

        event.parts().stream().map(ProductCreatedEvent.ProductPart::animalRegistrationNumber)
                .distinct().sorted().forEach(this::getAnimal);
        Product product = getProduct(event.productId());
        require(product.getParts().isEmpty() || product.getParts().stream()
                        .map(Part::getId).collect(java.util.stream.Collectors.toSet()).equals(partIds),
                "Product composition conflicts with an existing product");

        Map<Long, Tray> trays = new HashMap<>();
        event.parts().stream().map(ProductCreatedEvent.ProductPart::trayId).distinct().sorted()
                .forEach(id -> trays.put(id, getTray(id)));

        // All consumers lock trays before parts, and multiple IDs in ascending order.
        for (ProductCreatedEvent.ProductPart source : event.parts().stream()
                .sorted(Comparator.comparing(ProductCreatedEvent.ProductPart::partId)).toList())
        {
            Part part = getPart(source.partId(), getAnimal(source.animalRegistrationNumber()),
                    null, source.weight());
            Tray tray = trays.get(source.trayId());
            attachPartToTray(tray, part);

            Optional<Long> existingProduct = productRepo.findProductIdByPartId(part.getId());
            require(existingProduct.isEmpty() || existingProduct.get().equals(product.getId()),
                    "Part " + part.getId() + " already belongs to another product");
            product.getParts().add(part);
            product.getTrays().add(tray);
        }
    }

    private boolean beginEvent(UUID eventId)
    {
        if (processedEventRepo.existsById(eventId))
        {
            return false;
        }

        entityManager.persist(new ProcessedEvent(eventId));
        entityManager.flush();
        return true;
    }

    private Animal getAnimal(String registrationNumber)
    {
        return animalRepo.findById(registrationNumber).orElseGet(() -> {
            Animal animal = new Animal();
            animal.setRegistrationNumber(registrationNumber);
            entityManager.persist(animal);
            return animal;
        });
    }

    private Tray getTray(Long id)
    {
        return trayRepo.findByIdForUpdate(id).orElseGet(() -> {
            Tray tray = new Tray();
            tray.setId(id);
            entityManager.persist(tray);
            return tray;
        });
    }

    private Product getProduct(Long id)
    {
        return productRepo.findByIdForUpdate(id).orElseGet(() -> {
            Product product = new Product();
            product.setId(id);
            entityManager.persist(product);
            return product;
        });
    }

    private Part getPart(Long id, Animal animal, PartType type, BigDecimal weight)
    {
        // Station 2 can publish a weight before PostgreSQL rounds NUMERIC(10,2).
        BigDecimal normalizedWeight = weight.setScale(2, RoundingMode.HALF_UP);
        Optional<Part> existing = partRepo.findByIdForUpdate(id);
        Part part;
        if (existing.isPresent())
        {
            part = existing.get();
            // A collection may have loaded this part before its row lock was acquired.
            entityManager.refresh(part, LockModeType.PESSIMISTIC_WRITE);
        } else
        {
            part = new Part();
            part.setId(id);
            part.setAnimal(animal);
            entityManager.persist(part);
        }

        require(part.getAnimal().getRegistrationNumber().equals(animal.getRegistrationNumber()),
                "Part " + id + " has a conflicting animal");
        require(part.getWeight() == null || part.getWeight().compareTo(normalizedWeight) == 0,
                "Part " + id + " has a conflicting weight");
        require(type == null || part.getType() == null || part.getType() == type,
                "Part " + id + " has a conflicting type");

        part.setWeight(normalizedWeight);
        if (type != null)
        {
            part.setType(type);
        }

        return part;
    }

    private void attachPartToTray(Tray tray, Part part)
    {
        Optional<Long> existingTray = trayRepo.findTrayIdByPartId(part.getId());
        require(existingTray.isEmpty() || existingTray.get().equals(tray.getId()),
                "Part " + part.getId() + " has a conflicting tray");
        tray.getParts().add(part);
    }

    private void validateHeader(UUID id, String type, Instant occurredAt, String expectedType)
    {
        require(id != null && expectedType.equals(type) && occurredAt != null,
                "Missing or invalid event envelope");
    }

    private void validatePart(Long id, String registrationNumber, BigDecimal weight)
    {
        validateId(id);
        validateRegistrationNumber(registrationNumber);
        validateWeight(weight);
    }

    private void validateId(Long id)
    {
        require(id != null && id > 0, "IDs must be positive");
    }

    private void validateRegistrationNumber(String registrationNumber)
    {
        require(registrationNumber != null && !registrationNumber.isBlank()
                && registrationNumber.length() <= 100, "Invalid animal registration number");
    }

    private void validateWeight(BigDecimal weight)
    {
        require(weight != null && weight.signum() > 0, "Invalid part or tray weight");
        BigDecimal normalizedWeight = weight.setScale(2, RoundingMode.HALF_UP);
        require(normalizedWeight.signum() > 0
                && normalizedWeight.compareTo(new BigDecimal("99999999.99")) <= 0,
                "Invalid part or tray weight");
    }

    private void require(boolean condition, String message)
    {
        if (!condition)
        {
            throw new IllegalArgumentException(message);
        }
    }
}
