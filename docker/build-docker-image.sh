#!/bin/bash

# Navigate to project root directory
cd ..

# Run maven clean install
mvn clean install

# Build docker image
docker build -t order-manager:0.1 -f docker/Dockerfile .

echo "Build completed: order-manager:0.1"
