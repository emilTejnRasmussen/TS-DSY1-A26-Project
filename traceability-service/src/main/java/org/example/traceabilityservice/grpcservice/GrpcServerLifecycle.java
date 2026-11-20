package org.example.traceabilityservice.grpcservice;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class GrpcServerLifecycle implements SmartLifecycle
{
    private final Server server;
    private volatile boolean running;

    public GrpcServerLifecycle(TraceabilityGrpcService service,
                               @Value("${grpc.server.port:9090}") int port)
    {
        server = ServerBuilder.forPort(port).addService(service).build();
    }

    @Override
    public void start()
    {
        try
        {
            server.start();
            running = true;
            log.info("gRPC server started on port {}", server.getPort());
        } catch (IOException e)
        {
            throw new IllegalStateException("Cannot start the gRPC server", e);
        }
    }

    @Override
    public void stop()
    {
        server.shutdown();
        try
        {
            if (!server.awaitTermination(5, TimeUnit.SECONDS))
            {
                server.shutdownNow();
            }
        } catch (InterruptedException e)
        {
            server.shutdownNow();
            Thread.currentThread().interrupt();
        } finally
        {
            running = false;
        }
    }

    @Override
    public boolean isRunning()
    {
        return running;
    }

    public int getPort()
    {
        return server.getPort();
    }

    @Override
    public int getPhase()
    {
        return 0;
    }
}
