package org.example.packagingservice.service;

import org.example.packagingservice.dto.CreateProductRequest;
import org.example.packagingservice.dto.ProductResponse;
import org.example.packagingservice.entity.Part;
import org.example.packagingservice.entity.PartType;
import org.example.packagingservice.entity.Product;
import org.example.packagingservice.entity.Tray;
import org.example.packagingservice.exception.InvalidProductCompositionException;
import org.example.packagingservice.exception.PartAlreadyConsumedException;
import org.example.packagingservice.exception.PartNotFoundException;
import org.example.packagingservice.exception.ProductNotFoundException;
import org.example.packagingservice.mapper.PartMapper;
import org.example.packagingservice.mapper.ProductMapper;
import org.example.packagingservice.messaging.OutboxService;
import org.example.packagingservice.repository.PartRepository;
import org.example.packagingservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest
{
    @Mock
    private ProductRepository productRepo;

    @Mock
    private PartRepository partRepo;

    @Mock
    private OutboxService outboxService;

    private ProductService productService;

    @BeforeEach
    void setUp()
    {
        productService = new ProductService(
                productRepo, partRepo, new ProductMapper(new PartMapper()), outboxService
        );
    }

    @Test
    void createsProductFromPartsOfTheSameType()
    {
        Part first = part(1L, 10L, PartType.CHOP, "1.20", "ANIMAL-1");
        Part second = part(2L, 10L, PartType.CHOP, "0.30", "ANIMAL-2");
        givenAvailableParts(first, second);

        ProductResponse result = productService.createProduct(
                new CreateProductRequest("Chops", List.of(1L, 2L))
        );

        assertAll(
                () -> assertEquals(100L, result.id()),
                () -> assertEquals("Chops", result.name()),
                () -> assertEquals(new BigDecimal("1.50"), result.totalWeight()),
                () -> assertEquals(List.of(1L, 2L), result.parts().stream().map(p -> p.id()).toList()),
                () -> assertSame(first.getProduct(), second.getProduct()),
                () -> assertEquals(100L, first.getProduct().getId()),
                () -> assertTrue(result.parts().stream().allMatch(p -> p.productId().equals(100L)))
        );

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(outboxService).recordProductCreated(productCaptor.capture());
        assertSame(first.getProduct(), productCaptor.getValue());
        assertSame(first, productCaptor.getValue().getParts().getFirst());
    }

    @Test
    void createsMixedProductAndPreservesPartTrayAndAnimalReferences()
    {
        Part first = part(1L, 10L, PartType.CHOP, "0.10", "ANIMAL-1");
        Part second = part(2L, 20L, PartType.STEAK, "0.20", "ANIMAL-2");
        givenAvailableParts(first, second);

        ProductResponse result = productService.createProduct(
                new CreateProductRequest("Mixed cuts", List.of(2L, 1L))
        );

        assertAll(
                () -> assertEquals(new BigDecimal("0.30"), result.totalWeight()),
                () -> assertEquals(PartType.CHOP, result.parts().getFirst().type()),
                () -> assertEquals(10L, result.parts().getFirst().trayId()),
                () -> assertEquals("ANIMAL-1", result.parts().getFirst().animalRegistrationNumber()),
                () -> assertEquals(PartType.STEAK, result.parts().get(1).type()),
                () -> assertEquals(20L, result.parts().get(1).trayId()),
                () -> assertEquals("ANIMAL-2", result.parts().get(1).animalRegistrationNumber())
        );

        InOrder order = inOrder(partRepo, productRepo, outboxService);
        order.verify(partRepo).findByIdForUpdate(1L);
        order.verify(partRepo).findByIdForUpdate(2L);
        order.verify(productRepo).save(any(Product.class));
        order.verify(outboxService).recordProductCreated(any(Product.class));
    }

    @ParameterizedTest
    @MethodSource("invalidPartIds")
    void rejectsInvalidCompositionBeforeAccessingInventory(List<Long> partIds)
    {
        assertThrows(InvalidProductCompositionException.class, () -> productService.createProduct(
                new CreateProductRequest("Invalid", partIds)
        ));

        verifyNoInteractions(partRepo, productRepo, outboxService);
    }

    private static Stream<List<Long>> invalidPartIds()
    {
        return Stream.of(null, List.of(), List.of(1L, 1L), List.of(0L), List.of(-1L),
                Arrays.asList(1L, null));
    }

    @Test
    void rejectsUnknownPartWithoutCreatingProductOrEvent()
    {
        when(partRepo.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThrows(PartNotFoundException.class, () -> productService.createProduct(
                new CreateProductRequest("Unknown part", List.of(999L))
        ));

        verifyNoInteractions(productRepo, outboxService);
    }

    @Test
    void rejectsAlreadyConsumedPartWithoutChangingOtherParts()
    {
        Part available = part(1L, 10L, PartType.CHOP, "1.00", "ANIMAL-1");
        Part consumed = part(2L, 10L, PartType.CHOP, "1.00", "ANIMAL-1");
        Product previous = new Product();
        previous.setId(99L);
        consumed.setProduct(previous);
        when(partRepo.findByIdForUpdate(1L)).thenReturn(Optional.of(available));
        when(partRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(consumed));

        assertThrows(PartAlreadyConsumedException.class, () -> productService.createProduct(
                new CreateProductRequest("Conflict", List.of(2L, 1L))
        ));

        assertNull(available.getProduct());
        assertSame(previous, consumed.getProduct());
        verifyNoInteractions(productRepo, outboxService);
    }

    @Test
    void cannotConsumeTheSamePartInASecondProduct()
    {
        Part part = part(1L, 10L, PartType.CHOP, "1.00", "ANIMAL-1");
        givenAvailableParts(part);
        productService.createProduct(new CreateProductRequest("First", List.of(1L)));

        assertThrows(PartAlreadyConsumedException.class, () -> productService.createProduct(
                new CreateProductRequest("Second", List.of(1L))
        ));

        verify(productRepo, times(1)).save(any(Product.class));
        verify(outboxService, times(1)).recordProductCreated(any(Product.class));
    }

    @Test
    void rejectsUnknownProduct()
    {
        when(productRepo.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ProductNotFoundException.class, () -> productService.getProduct(999L));
    }

    @Test
    void listsProductsWithTheirPartsAndPagination()
    {
        Product product = new Product();
        product.setId(10L);
        product.setName("Chop");
        Part part = part(1L, 20L, PartType.CHOP, "1.25", "ANIMAL-1");
        part.setProduct(product);
        product.getParts().add(part);
        PageRequest pageable = PageRequest.of(1, 2);
        when(productRepo.findAll(pageable)).thenReturn(new PageImpl<>(List.of(product), pageable, 3));

        var result = productService.getProducts(pageable);

        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getNumber());
        assertEquals(new BigDecimal("1.25"), result.getContent().getFirst().totalWeight());
        assertEquals("ANIMAL-1", result.getContent().getFirst().parts().getFirst().animalRegistrationNumber());
    }

    private void givenAvailableParts(Part... parts)
    {
        for (Part part : parts)
        {
            when(partRepo.findByIdForUpdate(part.getId())).thenReturn(Optional.of(part));
        }

        when(productRepo.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            product.setId(100L);
            return product;
        });
    }

    private Part part(long id, long trayId, PartType type, String weight, String animal)
    {
        Tray tray = new Tray();
        tray.setId(trayId);
        tray.setType(type);
        Part part = new Part();
        part.setId(id);
        part.setTray(tray);
        part.setWeight(new BigDecimal(weight));
        part.setAnimalRegistrationNumber(animal);
        tray.getParts().add(part);
        return part;
    }
}
