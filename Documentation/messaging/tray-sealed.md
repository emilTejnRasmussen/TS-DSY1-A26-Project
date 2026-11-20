# TraySealed Event

## Overview

Animal Processing publishes this event when a populated tray is sealed. Packaging receives the tray and its parts as local inventory. Traceability records the parts and their source tray.

## Event Structure

```json
{
  "eventId": "46a8af42-a967-414e-9aaa-b0e45c7bf081",
  "eventType": "TraySealed",
  "occurredAt": "2026-11-19T10:10:00Z",
  "trayId": 10,
  "partType": "LOIN",
  "totalWeight": 4.00,
  "parts": [
    {
      "partId": 101,
      "animalRegistrationNumber": "DK-101",
      "weight": 1.25
    },
    {
      "partId": 102,
      "animalRegistrationNumber": "DK-102",
      "weight": 2.75
    }
  ]
}
```

All parts have the tray's `partType`. `totalWeight` is the sum of the part weights. IDs are assigned by Processing.

The [source record](../../animal-processing-service/src/main/java/org/example/animalprocessingservice/messaging/TraySealedEvent.java) defines the contract.

## RabbitMQ Configuration

| Setting | Value |
|---|---|
| Exchange | `slaughterhouse.events` |
| Routing key | `tray.sealed` |
| Packaging queue | `packaging.tray-sealed` |
| Traceability queue | `traceability.tray-sealed` |

## Event Consumption

Packaging validates and saves the tray and all its parts in one transaction. It skips tray IDs or event IDs it has already received, so redelivery cannot restore consumed inventory.

Traceability creates or enriches the tray, parts, and animal references. Existing relationships must agree with the incoming data.

See the [messaging overview](README.md) for publishing, duplicate handling, and failures.
