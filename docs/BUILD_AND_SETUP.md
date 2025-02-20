# Setup Guide

## Prerequisites

- Docker Engine
- JDK 17
- Maven

## Building the Services

The project includes a build script that handles compiling the application and building the necessary Docker images.
Here's how to use it:

1. Navigate to the `/docker` directory or open a terminal into:

2. Make the build script executable:
   ```bash
   chmod +x build-docker-image.sh
   ```

3. Run the build script:
   ```bash
   ./build-docker-image.sh
   ```
4. [Optional] Check if the image was created successfully:

```bash
docker images | grep order-manager
docker images | grep redis-custom
```

This script performs the following actions:

- Runs `mvn clean install` to build the Spring Boot application
- Builds the order-manager Docker image (version 0.1)
- Builds a custom Redis Docker image (version 7.2.4)

## Running the Services

Once the Docker images are built, you can start the entire stack using Docker Compose:

```bash
docker compose up -d
```

This command will start the following services:

- Order Manager (Spring Boot application) - Port 8080
- MariaDB (Database) - Port 3306
- Meilisearch (Search Engine) - Port 7700
- Redis (Message Queue) - Port 6379

## Verifying the Setup

To verify that all services are running correctly:

1. Check container status:
   ```bash
   docker compose ps
   ```
   All services should show as "Up" status

2. Verify the Order Manager service:
   ```bash
   curl http://localhost:8080/actuator/health
   ```
   Should return a health status response
