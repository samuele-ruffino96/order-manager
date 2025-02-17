# Build Script Documentation

## Overview

The `build-docker-image.sh` script automates the process of building the order-manager Spring Boot application and
creating its Docker image. The script handles Maven build and Docker images creation.

## Usage

1. Make sure the script is executable:
```bash
chmod +x build-docker-image.sh
```
2. Run the script from the `/docker` directory:
```bash
./build-docker-image.sh
```
## Verify Installation
Check if the image was created successfully:
```bash
docker images | grep order-manager
docker images | grep redis-custom
```
