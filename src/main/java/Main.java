import grpcservice.TraceabilityGrpcService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
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

    server.awaitTermination();
}