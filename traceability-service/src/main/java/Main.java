
import grpcservice.TraceabilityGrpcService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import messaging.AnimalEventConsumer;
import repository.PostgresTraceabilityRepository;
import repository.TraceabilityRepository;

void main() throws Exception
{
    TraceabilityRepository repository = new PostgresTraceabilityRepository();

    TraceabilityGrpcService traceabilityService = new TraceabilityGrpcService(repository);

    Server server = ServerBuilder
            .forPort(9090)
            .addService(traceabilityService)
            .build()
            .start();

    IO.println("gRPC server started on port 9090");

    try (AnimalEventConsumer consumer = new AnimalEventConsumer(repository))
    {
        consumer.start();

        IO.println("RabbitMQ consumer started");

        server.awaitTermination();
    }
    finally
    {
        server.shutdown();
    }
}
