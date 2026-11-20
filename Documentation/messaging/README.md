# Messaging

The services exchange JSON events through the durable RabbitMQ topic exchange `slaughterhouse.events`. Messages are persistent. Each consumer declares its own durable queues and bindings.

## Event Routes

| Event | Publisher | Routing key | Subscriber queues |
|---|---|---|---|
| [AnimalRegistered](animal-registered.md) | Animal Registration | `animal.registered` | `processing.animal-registered`, `traceability.animal-registered` |
| [PartRegistered](part-registered.md) | Animal Processing | `part.registered` | `traceability.part-registered` |
| [TraySealed](tray-sealed.md) | Animal Processing | `tray.sealed` | `packaging.tray-sealed`, `traceability.tray-sealed` |
| [ProductCreated](product-created.md) | Packaging | `product.created` | `traceability.product-created` |

Every event contains an `eventId` UUID, an `eventType`, and an `occurredAt` UTC timestamp. Retries keep the same event ID.

## Transactional Outbox

Each station saves its business changes and outgoing event in the same database transaction. A scheduled publisher checks for unpublished events five seconds after its previous run finishes.

The publisher sends the event and waits for a correlated RabbitMQ confirmation. Failed publications remain unpublished and are retried on later runs.

Stations 2 and 3 also check for returned messages before marking an event published. Station 1 checks the confirmation but does not check returned messages. Its confirmation alone does not prove that the event reached a queue. The required exchange, queues, and bindings must exist before publishing.

A crash after delivery but before marking an event published can cause redelivery. The current implementation assumes one publisher instance per station.

## Duplicate Handling

- Processing skips animal registration numbers it already knows.
- Packaging checks tray IDs and event IDs before saving incoming inventory. Repeated delivery does not recreate consumed parts.
- Traceability records the event ID in `processed_event` in the same transaction as its data changes. Repeated event IDs are skipped.

Traceability uses atomic inserts and row locks when concurrent events reference the same animal, part, tray, or product. Conflicting relationships are rejected rather than overwriting existing data.

## Out-of-Order Delivery

Traceability receives events through four separate queues. There is no guaranteed order between those queues.

A product event can arrive before its part or tray events. Traceability creates missing references from the product payload. Later events add details such as part type while preserving existing relationships. It does not call another service to retrieve missing data.

## Failed Deliveries

Traceability rolls back invalid or conflicting event changes, then stores the payload and reason in `failed_event`. The delivery is acknowledged after that record is saved. These failures require investigation. Database failures remain retryable through RabbitMQ redelivery.

Processing and Packaging do not have a `failed_event` table. Their listener failures require checking the service logs. No dead-letter queues are configured in the application code.

## Service Independence

Stations can work with already received data while other services or RabbitMQ are offline, provided their own backend and database are available. Outgoing events wait in the local outbox. A station cannot use inventory whose event has not arrived.

Durable queues retain pending events while consumers are offline. Traceability answers queries from its own database, but results can lag while events are pending. It has no outgoing events or outbox.
