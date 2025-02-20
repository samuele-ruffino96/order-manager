# API Guide and Test Data Setup

## Setting Up Test Data

First, let's populate the database with some sample products. You can execute this SQL either through a database client
or using the MariaDB command line tool.

### Connecting to MariaDB

```bash
docker exec -it <DB_CONTAINER_NAME_OR_ID> mariadb -u user -ppassword orders
```

### Creating Sample Products

Once connected, execute the following SQL to create sample products:

```sql
INSERT INTO products (id, name, description, price, stock_level, version, created_at, updated_at)
VALUES
('1a5b36a7-ef02-11ef-989f-0242ac120002', 'iPhone 14', '256GB Midnight Black', 999.99, 50, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('1a5b383c-ef02-11ef-989f-0242ac120002', 'MacBook Pro', '14" M2 Pro 1TB', 1999.99, 25, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('1a5b387c-ef02-11ef-989f-0242ac120002', 'AirPods Pro', '2nd Generation', 249.99, 100, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('1a5b388d-ef02-11ef-989f-0242ac120002', 'iPad Air', '256GB WiFi Space Gray', 749.99, 35, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('1a5b389d-ef02-11ef-989f-0242ac120002', 'Apple Watch', 'Series 8 45mm GPS', 429.99, 75, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

## API Examples

### Creating a New Order

To create a new order, send a POST request to the orders endpoint. Here are some example requests:

#### Example 1: Basic Order

```http
POST http://localhost:8080/api/v1/orders
Content-Type: application/json

{
  "customerName": "John Smith",
  "description": "Holiday season order",
  "items": [
    {
      "productId": "1a5b36a7-ef02-11ef-989f-0242ac120002",
      "quantity": 2
    },
    {
      "productId": "1a5b389d-ef02-11ef-989f-0242ac120002",
      "quantity": 1
    }
  ]
}
```

This creates an order for:

- 2 iPhone 14 devices
- 1 Apple Watch

#### Example 2: Multi-Product Order

```http
POST http://localhost:8080/api/v1/orders
Content-Type: application/json

{
  "customerName": "Alice Johnson",
  "description": "Complete Apple setup",
  "items": [
    {
      "productId": "1a5b383c-ef02-11ef-989f-0242ac120002",
      "quantity": 1
    },
    {
      "productId": "1a5b387c-ef02-11ef-989f-0242ac120002",
      "quantity": 1
    },
    {
      "productId": "1a5b388d-ef02-11ef-989f-0242ac120002",
      "quantity": 1
    }
  ]
}
```

This creates an order for:

- 1 MacBook Pro
- 1 AirPods Pro
- 1 iPad Air

### Retrieving Orders

#### Get All Orders

```http
GET http://localhost:8080/api/v1/orders
```

#### Get Orders with Pagination

```http
GET http://localhost:8080/api/v1/orders?page=0&size=10&sort=createdAt,desc
```

#### Get Orders with Filtering

```http
GET http://localhost:8080/api/v1/orders?customerName=John
```

#### Get Specific Order

```http
GET http://localhost:8080/api/v1/orders/{orderId}
```

### Managing Order Items

#### Cancel Specific Order Items

```http
DELETE http://localhost:8080/api/v1/order-items?ids=item1-uuid,item2-uuid
```

#### Cancel Entire Order

```http
DELETE http://localhost:8080/api/v1/orders/{orderId}
```
