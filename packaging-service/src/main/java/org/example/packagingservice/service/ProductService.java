package org.example.packagingservice.service;

import lombok.RequiredArgsConstructor;
import org.example.packagingservice.dto.CreateProductRequest;
import org.example.packagingservice.dto.ProductResponse;
import org.example.packagingservice.entity.Part;
import org.example.packagingservice.entity.Product;
import org.example.packagingservice.exception.*;
import org.example.packagingservice.mapper.ProductMapper;
import org.example.packagingservice.messaging.OutboxService;
import org.example.packagingservice.repository.PartRepository;
import org.example.packagingservice.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService
{
    private final ProductRepository productRepo;
    private final PartRepository partRepo;
    private final ProductMapper productMapper;
    private final OutboxService outboxService;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request)
    {
        if (request.partIds() == null || request.partIds().isEmpty()
                || request.partIds().stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(request.partIds()).size() != request.partIds().size())
        {
            throw new InvalidProductCompositionException();
        }

        List<Part> parts = new ArrayList<>();

        // Lock in a consistent order when requests contain overlapping parts.
        for (Long partId : request.partIds().stream().sorted().toList())
        {
            Part part = partRepo.findByIdForUpdate(partId)
                    .orElseThrow(() -> new PartNotFoundException(partId));

            if (part.getProduct() != null)
            {
                throw new PartAlreadyConsumedException(partId);
            }

            parts.add(part);
        }

        Product product = new Product();
        product.setName(request.name());
        product.setParts(parts);

        Product savedProduct = productRepo.save(product);

        for (Part part : parts)
        {
            part.setProduct(savedProduct);
        }

        outboxService.recordProductCreated(savedProduct);

        return productMapper.toDto(savedProduct);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(long productId)
    {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        return productMapper.toDto(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getProducts(Pageable pageable)
    {
        return productRepo.findAll(pageable).map(productMapper::toDto);
    }
}
