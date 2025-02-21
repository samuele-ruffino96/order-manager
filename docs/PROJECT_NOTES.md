# Order Manager `0.1`

## Project overview

This Order Manager Service is a backend application designed to handle order processing and stock management,.

### Technical Stack

The core technologies used in this project include:

- **Java 17** and **Spring Boot 3** - Main development platform
- **Redis** - Distributed caching, lock and message queuing
- **MariaDB** - Primary database

### Project Structure

The application follows a clean, domain-driven architecture organized in the following key packages:

```
com.company.app.ordermanager
├── config        # Configuration classes for Spring, Redis, Meilisearch, etc.
├── controller    # REST API endpoints
├── dto           # Data Transfer Objects
├── entity        # Domain entities (Order, Product, etc.)
├── exception     # Custom exceptions, handlers and error DTOs
├── messaging     # Message handling for stock management
├── repository    # Data access layer
├── search        # Full-text search functionalities
└── service       # Business logic
```

### Architectural Patterns

The project implements several key architectural patterns:

1. **Layer Separation**: The application separates concerns into controllers, services, and repositories, following the
   traditional n-tier architecture pattern.

2. **Interface-First Design**: Services and controllers are defined through interfaces, promoting loose coupling and
   making the system more maintainable and testable.

3. **Event-Driven Architecture**: Stock management is handled through an event-driven approach, allowing for
   asynchronous order processing.

4. **Repository Pattern**: Data access is abstracted through repository interfaces, leveraging Spring Data JPA's
   capabilities while allowing for custom query methods when needed.

5. **DTO Pattern**: The application uses Data Transfer Objects to separate the domain model from the API contract, as
   seen in `CreateOrderDto` and `CreateOrderItemDto`.

## Technical Implementation Details

### 1. Order Processing Flow

The order processing begins when a customer creates an order through the `OrderController`. Each order contains one or
more order items, which go through their own lifecycle independently. This approach allows for partial order fulfillment
while maintaining system reliability.

#### Order Lifecycle Management

When an order is created via `OrderServiceImpl`, it follows this sequence:

1. The order is initially created in a `PROCESSING` state
2. Each order item begins in a `PROCESSING` status
3. Stock reservation requests are sent asynchronously through Redis streams
4. Based on stock availability, items transition to either `CONFIRMED` or `CANCELLED` states

The order's overall status is computed dynamically based on its items' statuses, providing a clear view of the order's
current state within the application.

#### Integration with Stock System

The stock management integration uses an event-driven approach with message queues to handle stock updates
asynchronously. This architectural decision:

- Prevents blocking operations during order creation
- Handles concurrent stock modifications safely through distributed locks
- Provides automatic retry mechanisms for failed operations
- Maintains consistency between order status and stock levels

This design is implemented using Redis Streams as the message queue technology. The application remains modular enough
that other message queue solutions could be substituted if needed.

### 2. Stock Management Implementation

The stock management system tries to addresses one of the core challenges in order management applications: maintaining
accurate inventory levels while handling multiple concurrent order requests. The implementation focuses on three key
aspects: preventing race conditions, processing stock updates asynchronously, and managing concurrent access.

#### Distributed Locking Mechanism

To prevent race conditions when multiple orders attempt to reserve the same product's stock simultaneously, the
application uses a distributed locking strategy. When a stock update is needed, the system:

1. Acquires a lock specific to the product being updated
2. Performs the stock level check and update
3. Releases the lock

This mechanism ensures that stock modifications happen atomically, preventing overselling while maintaining system
performance. The implementation uses Redisson's `RLock`, which provides a robust distributed lock implementation with
features like automatic lock release (in case of client crashes) and lock timeouts.

#### Asynchronous Processing

Stock updates are handled asynchronously through a message queue system. When an order is placed:

1. The system publishes stock reservation messages to a queue
2. A dedicated consumer processes these messages asynchronously
3. Order status updates reflect the result of stock operations

This approach decouples order creation from stock processing, improving system responsiveness and resilience.

### 3. Caching Strategy

The caching strategy focuses on optimizing access to frequently requested data while ensuring data consistency across
the distributed system. The primary focus is on product stock levels, which require both high availability and strong
consistency.

#### Stock Level Caching

Instead of querying the database for each stock check, the system implements a caching layer that stores current stock
levels in memory. When a service needs to check product stock, it first consults the cache:

```java
public int getProductStockLevel(UUID productId) {
   String cachedStock = cache.get(productStockKey);

   if (cachedStock == null) {
      // Cache miss - fetch from database and update cache
      Product product = productRepository.findById(productId);
      cache.set(productStockKey, product.getStockLevel(), CACHE_DURATION);
      return product.getStockLevel();
   }

   return Integer.parseInt(cachedStock);
}
```

The cache is configured with a reasonable time-to-live (TTL) of one hour, balancing data freshness with system
performance. This approach significantly reduces database load for one of our most frequently accessed pieces of data.

#### Cache Consistency

To maintain cache consistency when stock levels change, it was followed a "write-through" caching pattern. When a stock
level update occurs, the system:

1. Update the database record
2. Immediately update the cache with the new value
3. Set an expiration time on the cached value as a safety mechanism

This strategy ensures that cache inconsistencies are short-lived while maintaining high performance for read operations.

### 4. Data Layer Design

#### Entity Relationships

The domain model centers around three main entities: `Order`, `OrderItem`, and `Product`. Their relationships are
structured to reflect real-world business rules.

An Order maintains a one-to-many relationship with OrderItems, while each OrderItem has a many-to-one relationship with
both Order and Product. This design allows for flexible order composition while maintaining data consistency. The
relationship is implemented with JPA annotations, with careful consideration of fetch strategies to prevent unnecessary
data fetching, improving application performance by loading related entities only when needed.

#### Dynamic Query Capabilities

To support flexible order searching and filtering, the system implements dynamic query generation using QueryDSL. This
allows the API to handle complex search criteria without hardcoding query patterns. The repository layer includes
predicate customization.

```java
public interface OrderRepository extends JpaRepository<Order, UUID>, QuerydslPredicateExecutor<Order> {
    // Custom bindings for search criteria
    void customize(QuerydslBindings bindings, QOrder order) {
        bindings.bind(order.description)
                .first(StringExpression::containsIgnoreCase);
    }
}
```

This approach provides a flexible, type-safe way to build complex queries while maintaining clean, maintainable code.

### 5. Full-Text Search Integration

The search functionality is implemented using Meilisearch, complementing our JPA-based data access with full-text search
capabilities. The combination of these components creates a robust search system.

#### Document

The search implementation follows a document-based approach, where domain entities are transformed into flattened search
documents. Consider as example the `OrderDocument` class:

```java
@Data
@Builder
public class OrderDocument {
   private UUID id;
   private String customerName;
   private String description;
   private Instant createdAt;
   private int totalItems;
}
```

This flattened document structure serves several purposes:

1. Optimizes data for search performance by including only searchable fields
2. Decouples the search index schema from our domain model
3. Allows independent evolution of search capabilities without affecting the core domain

The search integration is built around three key components that ensure clean separation of concerns:

**Document Mapping Strategy**  
The system uses a transformation layer that converts between domain entities and search documents. This mapping is
encapsulated in the `OrderDocument` class:

```java
public static OrderDocument fromEntity(Order order) {
   return OrderDocument.builder()
           .id(order.getId())
           .customerName(order.getCustomerName())
           .description(order.getDescription())
           .createdAt(order.getCreatedAt())
           .totalItems(order.getOrderItems().size())
           .build();
}
```

This approach ensures that search documents remain a pure projection of our domain model, making it easier to modify
either without affecting the other.

**Asynchronous Index Management**  
To prevent search operations from impacting core business transactions, index updates are performed asynchronously:

```java
@Async
public void indexOrder(Order order) {
   try {
      OrderDocument document = OrderDocument.fromEntity(order);
      String jsonDocument = objectMapper.writeValueAsString(document);
      orderIndex.addDocuments(jsonDocument);
      log.debug("Successfully indexed order: {}", order.getId());
   } catch (MeilisearchException e) {
      log.error("Failed to index order with id {} : {}",
              order.getId(), e.getMessage());
   }
}
```

This approach ensures that:

- Core business operations complete without waiting for search indexing
- Search index updates don't impact database transaction performance
- Failed index operations can be retried without affecting the main application flow

**Search Configuration and Optimization**  
The search functionality is fine-tuned through the configuration of Meilisearch attributes:

```java
private void configureIndex() {
   orderIndex.updateSearchableAttributesSettings(Arrays.asList(
           "customerName",
           "description"
   ).toArray(new String[0]));

   orderIndex.updateFilterableAttributesSettings(Arrays.asList(
           "createdAt"
   ).toArray(new String[0]));

   orderIndex.updateRankingRulesSettings(Arrays.asList(
           "words",
           "typo",
           "proximity",
           "attribute",
           "sort",
           "exactness"
   ).toArray(new String[0]));
}
```

This configuration specifies which fields support full-text search versus filtering and defines ranking rules to improve
result relevance

#### Request/Response Model

The search integration also supports date range filtering and pagination, implemented through a dedicated
request/response model:

```java
@Data
public class OrderSearchRequest {
   private String searchTerm;
   private Instant dateFrom;
   private Instant dateTo;
}

@Data
@Builder
public class OrderSearchResult {
   private UUID id;
   private String customerName;
   private String description;
   private Instant createdAt;
   private int totalItems;
}
```

The implementation handles conversion between search parameters and Meilisearch's native query format.

### 6. API Layer

The API layer serves as the interface between clients and our business logic, providing a RESTful service that follows
HTTP standards and best practices.

#### REST Endpoints Design

The REST API follows a resource-oriented architecture with clear naming conventions and appropriate HTTP method usage.
Let's take as example the `OrderController` to illustrates this approach:

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderControllerImpl {
    @GetMapping              // List orders with filtering
    @GetMapping("/search")   // Fulltext orders search 
    @GetMapping("/{id}")     // Get single order
    @PostMapping             // Create new order
    @DeleteMapping("/{id}")  // Cancel order
}
```

Each endpoint is documented with OpenAPI annotations, providing clear specifications for API consumers. The versioning
in the URL path (/v1/) allows for future API evolution while maintaining backward compatibility.

#### Response View Customization

The system implements a flexible response customization system using JSON Views to control what data is exposed in
different contexts. This allows us to reuse the same entities while providing different levels of detail based on the
use case:

```java
public interface JsonViews {
    class ListView {
    }               // Basic information

    class DetailView extends ListView {
    }  // Detailed information

    class InternalView extends DetailView {
    }  // Full internal data
}
```

For example, when listing orders, the REST API return basic information, but when retrieving a specific order, the
response includes additional details like order items and their statuses:

```java
@RestController
@RequestMapping("/api/v1/orders")
public class OrderControllerImpl implements OrderController {
   @GetMapping
   @JsonView(JsonViews.ListView.class)
   public Page<Order> getOrdersList(Predicate predicate, Pageable pageable) {...}

   @GetMapping("/{id}")
   @JsonView(JsonViews.DetailView.class)
   public Order getOrderById(UUID id) {...}
}
```

This approach reduces unnecessary data transfer and improves API performance.

#### Input Validation Strategy

Input validation was implemented using Jakarta Validation annotations on DTOs:

```java
public class CreateOrderDto {
    @NotBlank(message = "Customer name is required")
    private String customerName;

    @NotEmpty(message = "Order must contain at least one item")
    private Set<@Valid CreateOrderItemDto> items;
}
```

This validation ensures that:

- Required fields are present and properly formatted
- Numeric values are within acceptable ranges
- Complex objects are validated recursively
- Custom business rules are enforced before reaching the service layer

### 8. Error Handling

The application implements a comprehensive error handling strategy that ensures predictable behavior during failure
scenarios while providing meaningful feedback to API consumers. The approach spans multiple layers of the application to
create a robust error management system.

#### Exception Hierarchy

It was designed a domain-specific exception hierarchy that maps business scenarios to appropriate HTTP responses. For
example, when dealing with product-related operations, a specialized exception is used:

```java
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(UUID productId) {
        super(String.format("Product with ID %s not found", productId));
    }
}
```

This approach allows to maintain a clear separation between technical failures and business rule violations. Similar
exceptions exist for orders and inventory operations, each providing specific context about the failure.

#### Global Error Management

The application implements a comprehensive global exception handling strategy through a dedicated
`GlobalExceptionHandler` class annotated with `@RestControllerAdvice`. This centralized approach ensures consistent
error responses across all endpoints while providing appropriate levels of detail based on the error type.

The exception handler manages several categories of exceptions:

- Validation Errors: When request payloads fail Jakarta Validation constraints, the handler captures
  `MethodArgumentNotValidException` and transforms it into a structured response containing specific field-level
  validation errors. This helps clients quickly identify and correct invalid input data.

- Domain-Specific Exceptions: Custom exceptions like `ProductNotFoundException` and `OrderNotFoundException` are mapped
  to HTTP 404 responses with clear, business-focused error messages that help API consumers understand what resource was
  not found.

- Unexpected Errors: A catch-all handler for unhandled exceptions provides a sanitized error response, preventing
  sensitive implementation details from leaking to clients while maintaining a consistent error format.

This consistent error handling approach provides several benefits:

- Unified error response format across all endpoints
- Clear separation between validation errors and business logic exceptions
- Appropriate error details for debugging while maintaining security
- Improved API usability through descriptive error messages

#### Transactional Boundaries

Transaction management is crucial for maintaining data consistency, especially during stock operations. The application
carefully define transactional boundaries to ensure that related operations either complete entirely or roll back
completely. Consider stock reservation:

```java
@Transactional
public void processStockUpdateMessage(StockUpdateMessage message) {
    // If any part of this process fails, all changes are rolled back
    // 1. Update order item status
    // 2. Modify stock level
    // 3. Update cache
}
```

If any step fails, the entire operation is rolled back, preventing inconsistencies between order status and stock
levels. This transactional approach extends across service boundaries when necessary, ensuring data integrity even
during complex operations.

## Identified Issues and Proposed Solutions

While the current implementation provides a functioning order management system, certain areas would benefit from a more
robust approach to handle common challenges in distributed microservices architecture. Due to time constraints, these
improvements weren't implemented, but let's explore what could enhance the system's reliability and scalability.

### 1. Transaction Management in Order Creation

**Current Implementation**  
The order creation flow currently handles two critical operations separately:

1. Creating the order entities in the database
2. Publishing stock update messages to the message queue

**Potential Issue**  
If message publishing fails after order creation succeeds, the system enters an inconsistent state - orders exist but no
stock reservation attempts are made, leading to permanently pending orders.

**Proposed Solution: Transactional Outbox and Polling publisher Patterns**  
These patterns ensures atomicity between database updates and message publishing by:

1. Creating an OUTBOX table in the database
2. Including message insertion into this table within the same transaction as order creation
3. Implementing a separate process that polls the OUTBOX table and publishes messages to the actual message queue
4. Marking messages as published once successfully processed

This approach guarantees that either both operations succeed or both fail, maintaining system consistency.

### 2. Message Processing Idempotency

**Current Implementation**  
The `RedisStreamStockMessageConsumer` processes stock updates without guaranteeing idempotency. If a consumer fails
after processing but before acknowledgment, message redelivery could cause duplicate processing.

**Proposed Solution: Message Deduplication Tracking**  
Implement a message tracking system using either:

1. A standalone distributed cache storing recently processed message IDs
2. A distributed cache with asynchronous persistence to a database table (write-behind caching pattern)

This ensures that even if messages are redelivered, they won't be processed multiple times, maintaining stock level
accuracy.

### 3. Message Broker System Enhancement

**Current Implementation**  
The current use of Redis streams as a message broker requires significant manual handling of:

- Message partitioning based on product IDs (necessary to ensure all messages for the same product go to the same
  consumer)
- Consumer group management
- Stream cleanup and maintenance

**Proposed Solution: Modern Message Broker**

A system like Apache Kafka would provide several advantages:

1. Built-in partitioning ensures messages for the same product are processed in order
2. Automatic consumer group balancing
3. Configurable message retention and cleanup
4. Better scalability for high-volume message processing
5. Native support for message replay and fault tolerance

The current architecture's interface-based design makes this transition particularly straightforward through a clear
message handling interface contracts:

```java
public interface StockMessageProducerService {
    void sendStockReservationMessage(Set<OrderItem> orderItems);

    void sendStockCancellationMessage(Set<OrderItem> orderItems);
}

public interface StockMessageConsumerService {
    void processStockUpdateMessage(StockUpdateMessage message);
}
```

A potential Kafka implementation:

```java
@Service
public class KafkaStockMessageProducer implements StockMessageProducerService {
    private final KafkaTemplate<String, StockUpdateMessage> kafkaTemplate;

    @Override
    public void sendStockReservationMessage(Set<OrderItem> orderItems) {
        // Create stock update message list

        // Publish to Kafka topic
        messages.forEach(message -> kafkaTemplate.send("stock-updates", message.getProductId().toString(), message));
    }
}

@Service
public class KafkaStockMessageConsumer implements StockMessageConsumerService {
    @Override
    @KafkaListener(topics = "stock-updates", groupId = "stock-processor-group")
    public void processStockUpdateMessage(StockUpdateMessage message) {
        // Process message - same business logic as Redis implementation
        // Kafka handles message acknowledgment automatically
    }
}
```

This comparison demonstrates how:

1. The core business logic remains unchanged
2. Implementation details are isolated to specific classes
3. Different message brokers can be used without affecting other system components
4. The transition could be done gradually by running both implementations in parallel

The interface-based approach exemplifies how good architectural decisions early in development can make future system
evolution much simpler and safer.

## Testing

**TODO**: Describe testing classes subdivision (unit, integration test), the When-Given-Then pattern approach and the
test methods name's convention [operation]_when[condition]_should[expectedBehavior].
