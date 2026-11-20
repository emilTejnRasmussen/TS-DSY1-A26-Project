# Slaughterhouse System Documentation

This directory contains the architecture, domain model, sequence diagrams, database references, and messaging documentation for the distributed slaughterhouse system.

## Architecture

The C4 diagrams include a system context view (C1), container views (C2), and component views (C3) for all four services.

| Diagram | Description |
|---|---|
| [C1: System Context](architecture/C1-system-context.svg) | Overview of the slaughterhouse system and its external actors and systems |
| [C2: Animal Registration](architecture/C2-animal-registration.svg) | Containers and technologies used by the Animal Registration Service |
| [C2: Animal Processing](architecture/C2-animal-processing-system.svg) | Containers and technologies used by the Animal Processing Service |
| [C2: Packaging](architecture/C2-packaging-system.svg) | Containers and technologies used by the Packaging Service |
| [C2: Traceability](architecture/C2-traceability.svg) | Containers and technologies used by the Traceability Service |
| [C3: Animal Registration](architecture/C3-animal-registration.svg) | Internal components of the Animal Registration Service |
| [C3: Animal Processing](architecture/C3-animal-processing.svg) | Internal components of the Animal Processing Service |
| [C3: Packaging](architecture/C3-packaging.svg) | Internal components of the Packaging Service |
| [C3: Traceability](architecture/C3-traceability.svg) | Internal components of the Traceability Service |

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
| [Traceability Schema](../traceability-service/src/main/resources/db/schema.sql) | Current schema for an empty Traceability database |
| [Traceability Migration](../traceability-service/src/main/resources/db/migrate-jdbc-to-jpa.sql) | Migration from the earlier JDBC schema to the current JPA schema |

All four backends use Spring Data JPA and Hibernate. Each owns a separate PostgreSQL database:

| Service | Database | Schema handling |
|---|---|---|
| Animal Registration | `animal_registration` | Hibernate `update` |
| Animal Processing | `animal_processing` | Hibernate `update` |
| Packaging | `packaging` | Hibernate `update` |
| Traceability | `traceability` | Hibernate `validate`; apply the schema or migration manually |

Traceability stores exact product-to-part links in `product_part`. It also retains source tray relationships in `product_tray`.

The older [schema](database/traceability-schema.sql) and [test data](database/traceability-test-data.sql) are historical JDBC examples, not current setup scripts. They record source trays without exact product-to-part links. Those links cannot reliably be reconstructed from the old tray relationships alone.

## Messaging

[Messaging Overview](messaging/README.md)

Documents event routes, outbox publishing, duplicate handling, and out-of-order delivery.

| Event | Published by | Consumed by |
|---|---|---|
| [AnimalRegistered](messaging/animal-registered.md) | Animal Registration | Animal Processing, Traceability |
| [PartRegistered](messaging/part-registered.md) | Animal Processing | Traceability |
| [TraySealed](messaging/tray-sealed.md) | Animal Processing | Packaging, Traceability |
| [ProductCreated](messaging/product-created.md) | Packaging | Traceability |
