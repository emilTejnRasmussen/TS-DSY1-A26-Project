package org.example.traceabilityservice.grpcservice;

import grpc.*;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.example.traceabilityservice.exception.IncompleteTraceabilityException;
import org.example.traceabilityservice.service.TraceabilityQueryService;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TraceabilityGrpcService extends TraceabilityServiceGrpc.TraceabilityServiceImplBase
{
    private final TraceabilityQueryService queryService;

    @Override
    public void getAnimalsForProduct(
            ProductRequest request,
            StreamObserver<AnimalRegistrationsResponse> responseObserver)
    {
        try
        {
            List<String> registrationNumbers = queryService.getAnimalsForProduct(request.getProductId());

            responseObserver.onNext(AnimalRegistrationsResponse.newBuilder()
                    .addAllRegistrationNumbers(registrationNumbers)
                    .build());
            responseObserver.onCompleted();
        } catch (IncompleteTraceabilityException e)
        {
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription(e.getMessage()).asRuntimeException());
        } catch (DataAccessException | TransactionException e)
        {
            responseObserver.onError(Status.INTERNAL.withDescription("Database error")
                    .withCause(e).asRuntimeException());
        }
    }

    @Override
    public void getProductsForAnimal(
            AnimalRequest request,
            StreamObserver<ProductsResponse> responseObserver)
    {
        try
        {
            List<Integer> productIds = queryService.getProductsForAnimal(request.getRegistrationNumber())
                    .stream()
                    .map(Math::toIntExact)
                    .toList();

            responseObserver.onNext(ProductsResponse.newBuilder().addAllProductIds(productIds).build());
            responseObserver.onCompleted();
        } catch (IncompleteTraceabilityException e)
        {
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription(e.getMessage()).asRuntimeException());
        } catch (ArithmeticException e)
        {
            responseObserver.onError(Status.OUT_OF_RANGE
                    .withDescription("A product ID exceeds the int32 range of the current gRPC contract.")
                    .withCause(e).asRuntimeException());
        } catch (DataAccessException | TransactionException e)
        {
            responseObserver.onError(Status.INTERNAL.withDescription("Database error")
                    .withCause(e).asRuntimeException());
        }
    }
}
