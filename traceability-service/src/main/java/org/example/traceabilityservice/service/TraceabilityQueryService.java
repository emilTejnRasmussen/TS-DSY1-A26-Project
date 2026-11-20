package org.example.traceabilityservice.service;

import lombok.RequiredArgsConstructor;
import org.example.traceabilityservice.exception.IncompleteTraceabilityException;
import org.example.traceabilityservice.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TraceabilityQueryService
{
    private final ProductRepository productRepo;

    public List<String> getAnimalsForProduct(long productId)
    {
        if (productId <= 0)
        {
            return List.of();
        }

        if (productRepo.existsWithoutPartLinks(productId))
        {
            throw new IncompleteTraceabilityException(
                    "Part-level traceability is unavailable for this legacy product. "
                            + "Replay its original ProductCreated event."
            );
        }

        return productRepo.findAnimalRegistrationNumbersByProductId(productId);
    }

    public List<Long> getProductsForAnimal(String registrationNumber)
    {
        if (registrationNumber == null || registrationNumber.isBlank())
        {
            return List.of();
        }

        if (productRepo.existsLegacyProductForAnimal(registrationNumber))
        {
            throw new IncompleteTraceabilityException(
                    "Some legacy products have only tray-level references for this animal. "
                            + "Replay their original ProductCreated events before requesting precise recall."
            );
        }

        return productRepo.findProductIdsByAnimalRegistrationNumber(registrationNumber);
    }
}
