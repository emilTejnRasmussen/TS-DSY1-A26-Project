# Slaughterhouse System

A distributed slaughterhouse system developed as part of the Distributed Systems course at VIA University College.

The project explores distributed system architectures through the development of a slaughterhouse system, covering animal registration, processing, packaging, and traceability.

The system is developed incrementally across multiple assignments, introducing new functionality and distributed systems concepts along the way.

## System Overview

The slaughterhouse process is divided into three stations:

| Station | Responsibility |
|---|---|
| Station 1 | Register animals arriving at the slaughterhouse |
| Station 2 | Process animals into parts and organize them into trays |
| Station 3 | Package products using trays of processed meat |

A separate Traceability Service maintains the relationships between animals, parts, trays, and products, allowing products to be traced back to their originating animals.

## Project Structure

```text
slaughterhouse/
├── animal-registration-service/
├── traceability-service/
├── frontend/
│   └── station-1/
├── Documentation/
└── pom.xml
```

## Technologies

The project uses a combination of technologies to explore different approaches to distributed systems.

- Java and Spring Boot
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

Future assignments will extend the system with additional functionality and distributed systems concepts.

## Documentation

Architecture diagrams, domain models, sequence diagrams, database schemas, and messaging documentation can be found in the [Documentation](Documentation/README.md) directory.
