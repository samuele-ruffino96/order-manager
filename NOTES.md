# Note

## Problemi noti

- `OrderService::createOrder`:
    - We need to publish messages as part of the transaction that create order and order items to prevent inconsistent
      order state,
      such as stuck orders management. We could use a simple approach like: Transactional outbox pattern + Polling
      publisher pattern.
- `StockStreamProcessor`:
    - If a consumer processes a stock update but fails to send the acknowledgment
      (for example, due to a crash or network issue), the message remains unacknowledged
      and will eventually be redelivered to another consumer in the group.
      This can lead to the same update being applied a second time.
      Wen can't have idempotent handler in this case so
      we need to tracking received messages and discarding Duplicates.
      Maybe we could use redis to cache last N received message
    - Gestione delle partizioni
    - Necessità di fare polling
