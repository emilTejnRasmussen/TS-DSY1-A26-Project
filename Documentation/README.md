# Slaughterhouse System Documentation

This directory contains the architecture, domain model, sequence diagrams, database schemas, and messaging documentation for the distributed slaughterhouse system.

## Architecture

The architecture is documented using the C4 model.

| Diagram                                                            | Description |
|--------------------------------------------------------------------|---|
| [C1: System Context](architecture/C1-system-context.svg)           | Overview of the slaughterhouse system and its external actors and systems |
| [C2: Animal Registration](architecture/C2-animal-registration.svg) | Containers and technologies used by the Animal Registration Service |
| [C2: Traceability](architecture/C2-traceability.svg)               | Containers and technologies used by the Traceability Service |
| [C3: Animal Registration](architecture/C3-animal-registration.svg) | Internal components of the Animal Registration Service |
| [C3: Traceability](architecture/C3-traceability.svg)               | Internal components of the Traceability Service |

## Domain Model

[Domain Model](domain/domain-model.svg)

Describes the main business entities and their relationships, from animal registration to packaged products.

## Sequence Diagrams

| Diagram | Description |
|---|---|
| [Animal Registration](sequences/animal-registration.svg) | Registration of an animal, outbox publishing, and asynchronous processing in Traceability |
| [Get Animals for Product](sequences/get-animals-for-product.svg) | Retrieving animal registration numbers associated with a product using gRPC |

## Database

| File | Description |
|---|---|
| [Animal Registration ERD](database/animal-registration-erd.svg) | Database structure for farms, animals, and outbox events |
| [Traceability ERD](database/traceability-erd.svg) | Database structure for animal traceability and processed events |
| [Traceability Schema](database/traceability-schema.sql) | SQL definitions for the Traceability database |
| [Traceability Test Data](database/traceability-test-data.sql) | Sample data for testing traceability queries |

The Animal Registration database is managed by Spring Data JPA and Hibernate. The Traceability database uses a manually defined PostgreSQL schema.

## Messaging

[AnimalRegistered Event](messaging/animal-registered.md)

Documents the RabbitMQ event contract, exchange and queue configuration, transactional outbox, and duplicate event handling.

The Traceability Service declares its RabbitMQ exchange, queue, and binding automatically at startup.