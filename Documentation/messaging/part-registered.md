# PartRegistered Event

## Overview

Animal Processing publishes this event when a part is registered. Traceability stores the part's originating animal, type, and weight.

## Event Structure

```json
{
  "eventId": "b63aad84-c198-4c57-9ded-8f05e33fbf3a",
  "eventType": "PartRegistered",
  "occurredAt": "2026-11-19T10:05:00Z",
  "partId": 101,
  "animalRegistrationNumber": "DK-101",
  "partType": "LOIN",
  "weight": 1.25
}
```

`partId` is assigned by Processing. `animalRegistrationNumber` identifies the originating animal. `partType` uses the Processing `PartType` enum.

The [source record](../../animal-processing-service/src/main/java/org/example/animalprocessingservice/messaging/PartRegisteredEvent.java) defines the contract.

## RabbitMQ Configuration

| Setting | Value |
|---|---|
| Exchange | `slaughterhouse.events` |
| Routing key | `part.registered` |
| Traceability queue | `traceability.part-registered` |

## Event Consumption

Traceability creates or enriches the part and its animal reference. A part can already be known from a tray or product event. Later registration must agree with the existing animal and weight.

See the [messaging overview](README.md) for publishing, duplicate handling, and failures.
