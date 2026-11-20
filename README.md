# Slaughterhouse System

A distributed slaughterhouse system developed as part of the Distributed Systems course at VIA University College.

The system covers animal registration, processing, packaging, and traceability. Each service owns its database and communicates with other services through RabbitMQ events.

## System Overview

The slaughterhouse process is divided into three stations:

| Station | Service | Responsibility |
|---|---|---|
| Station 1 | Animal Registration | Register animals and their originating farms |
| Station 2 | Animal Processing | Register parts, assign them to trays, and seal trays |
| Station 3 | Packaging | Receive sealed trays and create products from selected parts |

A separate Traceability Service provides two gRPC queries: find the animals involved in a product, and find the products involving an animal. Queries follow the exact parts used in each product. Different products can use different parts from the same tray.

Each station has its own frontend, which communicates only with that station's backend.

| Application | Backend | Frontend |
|---|---|---|
| Animal Registration | REST `http://localhost:8080/api/v1` | `http://localhost:5173` |
| Animal Processing | REST `http://localhost:8081/api/v1` | `http://localhost:5174` |
| Packaging | REST `http://localhost:8082/api/v1` | `http://localhost:5175` |
| Traceability | gRPC `localhost:9090` | No frontend |

Stations can continue working with locally available data while other services or RabbitMQ are offline. Outgoing events remain in local outboxes until they can be published. Traceability uses its own database, so its results can lag while events are pending.

## Project Structure

```text
slaughterhouse/
├── animal-registration-service/
├── animal-processing-service/
├── packaging-service/
├── traceability-service/
├── frontend/
│   ├── station-1/
│   ├── station-2/
│   └── station-3/
├── Documentation/
└── pom.xml
```

## Technologies

- Java and Spring Boot
- Spring Data JPA and Hibernate
- REST and gRPC
- PostgreSQL
- RabbitMQ
- React and TypeScript
- Maven and Docker

## Development Progress

The system is developed incrementally through course assignments.

| Assignment | Focus |
|---|---|
| Assignment 1 | Initial system design|
| Assignment 2 | Traceability functionality |
| Assignment 3 | Animal Registration Service, Station 1 frontend, RabbitMQ integration, and transactional outbox |
| Assignment 3.5 | Processing and Packaging services, JPA Traceability, and frontends for Stations 2 and 3 |

## Documentation

Architecture diagrams, the domain model, sequence diagrams, database references, and messaging documentation can be found in the [Documentation](Documentation/README.md) directory.

The C4 diagrams include a system context view (C1), container views (C2), and component views (C3) for all four services.
