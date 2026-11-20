package org.example.traceabilityservice.grpcservice;

import grpc.AnimalRegistrationsResponse;
import grpc.AnimalRequest;
import grpc.ProductRequest;
import grpc.ProductsResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.example.traceabilityservice.exception.IncompleteTraceabilityException;
import org.example.traceabilityservice.repository.ProductRepository;
import org.example.traceabilityservice.service.TraceabilityQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.transaction.CannotCreateTransactionException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TraceabilityGrpcServiceTest
{
    @Mock private TraceabilityQueryService queryService;
    @Mock private ProductRepository productRepo;

    private TraceabilityGrpcService grpcService;

    @BeforeEach
    void setUp()
    {
        grpcService = new TraceabilityGrpcService(queryService);
    }

    @Test
    void returnsAnimalsFromThePreciseQuery()
    {
        when(queryService.getAnimalsForProduct(1)).thenReturn(List.of("ANIMAL-A", "ANIMAL-B"));
        RecordingObserver<AnimalRegistrationsResponse> observer = new RecordingObserver<>();

        grpcService.getAnimalsForProduct(ProductRequest.newBuilder().setProductId(1).build(), observer);

        assertNull(observer.error);
        assertTrue(observer.completed);
        assertEquals(List.of("ANIMAL-A", "ANIMAL-B"), observer.response.getRegistrationNumbersList());
    }

    @Test
    void returnsProductIdsWithoutChangingThePublicMessage()
    {
        when(queryService.getProductsForAnimal("ANIMAL-A")).thenReturn(List.of(1L, 2L));
        RecordingObserver<ProductsResponse> observer = new RecordingObserver<>();

        grpcService.getProductsForAnimal(AnimalRequest.newBuilder().setRegistrationNumber("ANIMAL-A").build(), observer);

        assertNull(observer.error);
        assertTrue(observer.completed);
        assertEquals(List.of(1, 2), observer.response.getProductIdsList());
    }

    @Test
    void unknownProductReturnsAnEmptySuccessfulResponse()
    {
        when(queryService.getAnimalsForProduct(999)).thenReturn(List.of());
        RecordingObserver<AnimalRegistrationsResponse> observer = new RecordingObserver<>();

        grpcService.getAnimalsForProduct(ProductRequest.newBuilder().setProductId(999).build(), observer);

        assertTrue(observer.completed);
        assertNull(observer.error);
        assertEquals(0, observer.response.getRegistrationNumbersCount());
    }

    @Test
    void omittedProductIdPreservesTheEmptyResponse()
    {
        TraceabilityGrpcService service = new TraceabilityGrpcService(new TraceabilityQueryService(productRepo));
        RecordingObserver<AnimalRegistrationsResponse> observer = new RecordingObserver<>();

        service.getAnimalsForProduct(ProductRequest.getDefaultInstance(), observer);

        assertTrue(observer.completed);
        assertNull(observer.error);
        assertEquals(0, observer.response.getRegistrationNumbersCount());
        verifyNoInteractions(productRepo);
    }

    @Test
    void omittedAnimalRegistrationPreservesTheEmptyResponse()
    {
        TraceabilityGrpcService service = new TraceabilityGrpcService(new TraceabilityQueryService(productRepo));
        RecordingObserver<ProductsResponse> observer = new RecordingObserver<>();

        service.getProductsForAnimal(AnimalRequest.getDefaultInstance(), observer);

        assertTrue(observer.completed);
        assertNull(observer.error);
        assertEquals(0, observer.response.getProductIdsCount());
        verifyNoInteractions(productRepo);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, Integer.MIN_VALUE})
    void invalidProductIdsReturnNoResultsWithoutQuerying(long productId)
    {
        assertTrue(new TraceabilityQueryService(productRepo).getAnimalsForProduct(productId).isEmpty());
        verifyNoInteractions(productRepo);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void invalidAnimalRegistrationsReturnNoResultsWithoutQuerying(String registrationNumber)
    {
        assertTrue(new TraceabilityQueryService(productRepo).getProductsForAnimal(registrationNumber).isEmpty());
        verifyNoInteractions(productRepo);
    }

    @Test
    void productDatabaseErrorsPreserveInternalStatus()
    {
        when(queryService.getAnimalsForProduct(1)).thenThrow(new DataAccessResourceFailureException("Offline"));
        RecordingObserver<AnimalRegistrationsResponse> observer = new RecordingObserver<>();

        grpcService.getAnimalsForProduct(ProductRequest.newBuilder().setProductId(1).build(), observer);

        assertEquals(Status.Code.INTERNAL, Status.fromThrowable(observer.error).getCode());
        assertEquals("Database error", Status.fromThrowable(observer.error).getDescription());
        assertNull(observer.response);
        assertFalse(observer.completed);
    }

    @Test
    void animalDatabaseErrorsPreserveInternalStatus()
    {
        when(queryService.getProductsForAnimal("ANIMAL-A"))
                .thenThrow(new DataAccessResourceFailureException("Offline"));
        RecordingObserver<ProductsResponse> observer = new RecordingObserver<>();

        grpcService.getProductsForAnimal(AnimalRequest.newBuilder().setRegistrationNumber("ANIMAL-A").build(), observer);

        assertEquals(Status.Code.INTERNAL, Status.fromThrowable(observer.error).getCode());
        assertEquals("Database error", Status.fromThrowable(observer.error).getDescription());
        assertNull(observer.response);
        assertFalse(observer.completed);
    }

    @Test
    void productTransactionStartupFailurePreservesInternalStatus()
    {
        when(queryService.getAnimalsForProduct(1))
                .thenThrow(new CannotCreateTransactionException("Database unavailable"));
        RecordingObserver<AnimalRegistrationsResponse> observer = new RecordingObserver<>();

        grpcService.getAnimalsForProduct(ProductRequest.newBuilder().setProductId(1).build(), observer);

        assertEquals(Status.Code.INTERNAL, Status.fromThrowable(observer.error).getCode());
        assertEquals("Database error", Status.fromThrowable(observer.error).getDescription());
        assertNull(observer.response);
        assertFalse(observer.completed);
    }

    @Test
    void animalTransactionStartupFailurePreservesInternalStatus()
    {
        when(queryService.getProductsForAnimal("ANIMAL-A"))
                .thenThrow(new CannotCreateTransactionException("Database unavailable"));
        RecordingObserver<ProductsResponse> observer = new RecordingObserver<>();

        grpcService.getProductsForAnimal(AnimalRequest.newBuilder().setRegistrationNumber("ANIMAL-A").build(), observer);

        assertEquals(Status.Code.INTERNAL, Status.fromThrowable(observer.error).getCode());
        assertEquals("Database error", Status.fromThrowable(observer.error).getDescription());
        assertNull(observer.response);
        assertFalse(observer.completed);
    }

    @Test
    void legacyProductDoesNotPretendToHaveCompleteTraceability()
    {
        when(queryService.getAnimalsForProduct(1)).thenThrow(new IncompleteTraceabilityException("Replay required"));
        RecordingObserver<AnimalRegistrationsResponse> observer = new RecordingObserver<>();

        grpcService.getAnimalsForProduct(ProductRequest.newBuilder().setProductId(1).build(), observer);

        assertEquals(Status.Code.FAILED_PRECONDITION, Status.fromThrowable(observer.error).getCode());
        assertNull(observer.response);
        assertFalse(observer.completed);
    }

    @Test
    void legacyAnimalRecallDoesNotReturnAnIncompleteProductList()
    {
        when(queryService.getProductsForAnimal("ANIMAL-A"))
                .thenThrow(new IncompleteTraceabilityException("Replay required"));
        RecordingObserver<ProductsResponse> observer = new RecordingObserver<>();

        grpcService.getProductsForAnimal(AnimalRequest.newBuilder().setRegistrationNumber("ANIMAL-A").build(), observer);

        assertEquals(Status.Code.FAILED_PRECONDITION, Status.fromThrowable(observer.error).getCode());
        assertNull(observer.response);
        assertFalse(observer.completed);
    }

    @Test
    void productIdsOutsideThePublicInt32RangeAreNotTruncated()
    {
        when(queryService.getProductsForAnimal("ANIMAL-A")).thenReturn(List.of((long) Integer.MAX_VALUE + 1));
        RecordingObserver<ProductsResponse> observer = new RecordingObserver<>();

        grpcService.getProductsForAnimal(AnimalRequest.newBuilder().setRegistrationNumber("ANIMAL-A").build(), observer);

        assertEquals(Status.Code.OUT_OF_RANGE, Status.fromThrowable(observer.error).getCode());
        assertNull(observer.response);
        assertFalse(observer.completed);
    }

    private static class RecordingObserver<T> implements StreamObserver<T>
    {
        private T response;
        private Throwable error;
        private boolean completed;

        @Override
        public void onNext(T value)
        {
            response = value;
        }

        @Override
        public void onError(Throwable error)
        {
            this.error = error;
        }

        @Override
        public void onCompleted()
        {
            completed = true;
        }
    }
}
