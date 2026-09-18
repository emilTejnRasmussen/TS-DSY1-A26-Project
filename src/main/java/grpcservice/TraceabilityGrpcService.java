package grpcservice;


import grpc.*;
import io.grpc.stub.StreamObserver;
import repository.TraceabilityRepository;

import java.sql.SQLException;
import java.util.List;

public class TraceabilityGrpcService extends TraceabilityServiceGrpc.TraceabilityServiceImplBase
{
    private final TraceabilityRepository repository;

    public TraceabilityGrpcService(TraceabilityRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public void getAnimalsForProduct(
            ProductRequest request,
            StreamObserver<AnimalRegistrationsResponse> responseObserver)
    {
        try
        {
            List<String> registrationNumbers =
                    repository.findAnimalRegistrationNumbersByProductId(
                            request.getProductId()
                    );

            AnimalRegistrationsResponse response =
                    AnimalRegistrationsResponse.newBuilder()
                            .addAllRegistrationNumbers(registrationNumbers)
                            .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (SQLException e)
        {
            responseObserver.onError(e);
        }
    }
}