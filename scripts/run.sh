#!/bin/bash
set -e

echo "Starting User management Service with Docker Compose..."
docker compose up -d
echo "Service is running!"
echo "API available at http://localhost:8080"
echo "Database: PostgreSQL at localhost:5432"
echo ""
echo "To view logs, run: docker-compose logs -f"
echo "To stop the service, run: docker-compose down"

