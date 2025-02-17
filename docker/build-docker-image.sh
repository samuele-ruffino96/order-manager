#!/bin/bash

# Navigate to project root directory
cd ..

# Run maven clean install
mvn clean install

# Build order-manager docker image
docker build -t order-manager:0.1 -f docker/order-manager/Dockerfile .

echo "Build completed: order-manager:0.1"

# Build redis-custom docker image
docker build -t redis-custom:7.2.4 -f docker/redis/Dockerfile docker/redis

echo "Build completed: redis-custom:7.2.4"


