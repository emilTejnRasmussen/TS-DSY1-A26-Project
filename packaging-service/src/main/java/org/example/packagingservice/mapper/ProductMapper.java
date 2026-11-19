package org.example.packagingservice.mapper;

import lombok.RequiredArgsConstructor;
import org.example.packagingservice.dto.ProductResponse;
import org.example.packagingservice.entity.Product;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductMapper
{
    private final PartMapper partMapper;

    public ProductResponse toDto(Product product)
    {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getTotalWeight(),
                product.getParts().stream().map(partMapper::toDto).toList(),
                product.getCreatedAt()
        );
    }
}
