# ProductCreated Event

## Overview

Packaging publishes this event when a product is created from selected parts. Traceability records the exact product-to-part relationships and source trays.

## Event Structure

```json
{
  "eventId": "6f770f75-5820-46e6-a357-f2700a8273f6",
  "eventType": "ProductCreated",
  "occurredAt": "2026-11-19T10:15:00Z",
  "productId": 1,
  "parts": [
    {
      "partId": 101,
      "trayId": 10,
      "animalRegistrationNumber": "DK-101",
      "weight": 1.25
    }
  ]
}
```

`productId` is assigned by Packaging. Each entry identifies a consumed part, its source tray, its originating animal, and its weight. The event does not include the product name or part type.

The [source record](../../packaging-service/src/main/java/org/example/packagingservice/messaging/ProductCreatedEvent.java) defines the contract.

## RabbitMQ Configuration

| Setting | Value |
|---|---|
| Exchange | `slaughterhouse.events` |
| Routing key | `product.created` |
| Traceability queue | `traceability.product-created` |

## Event Consumption

Traceability saves the product's selected parts and source trays. Missing animal, part, and tray references can be created from this payload before earlier events arrive.

A tray can supply parts to several products. In this example, Product 1 contains Part 101 from Animal DK-101. It does not trace to other animals simply because their parts share Tray 10.

See the [messaging overview](README.md) for publishing, duplicate handling, and failures.
