#!/bin/bash

# Wait for database to be ready
echo "Waiting for PostgreSQL to be ready..."
while ! nc -z postgres 5432; do
  sleep 0.1
done
echo "PostgreSQL is ready"

# Initialize migrations directory if it doesn't exist
flask db init || true

# Run migrations
flask db migrate
flask db upgrade

# Start the application
exec gunicorn -w 4 -b 0.0.0.0:5000 app:app