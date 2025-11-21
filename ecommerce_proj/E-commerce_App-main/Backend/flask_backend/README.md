# E-commerce Flask Backend

A complete Flask backend for the e-commerce application with PostgreSQL database and Elasticsearch integration.

## Quick Start with Docker Compose

1. From the repository root (where docker-compose.yml lives), run:
   ```bash
   docker compose up --build
   ```

2. Services will be available at:
   - Flask API: http://localhost:5000
   - PostgreSQL: localhost:5432
   - Elasticsearch: http://localhost:9200
   - Kibana: http://localhost:5601

## API Endpoints

### Authentication
- POST `/api/auth/register`
  ```json
  {
    "username": "string",
    "email": "string",
    "password": "string",
    "role": "string" // optional, defaults to "user"
  }
  ```
- POST `/api/auth/login`
  ```json
  {
    "email": "string",
    "password": "string"
  }
  ```

### Products
- GET `/api/products?page=1&per_page=10&category_id=1`
- GET `/api/products/{product_id}`
- POST `/api/products` (Admin only)
  ```json
  {
    "name": "string",
    "description": "string",
    "price": number,
    "stock": number,
    "category_id": number
  }
  ```

### Categories
- GET `/api/categories`
- POST `/api/categories` (Admin only)
  ```json
  {
    "name": "string",
    "description": "string"
  }
  ```

### Cart
- GET `/api/cart`
- POST `/api/cart/items`
  ```json
  {
    "product_id": number,
    "quantity": number
  }
  ```

### Orders
- GET `/api/orders`
- GET `/api/orders/{order_id}`
- POST `/api/orders`
  ```json
  {
    "address": {
      "street": "string",
      "city": "string",
      "state": "string",
      "postal_code": "string",
      "country": "string"
    }
  }
  ```

### Search
- GET `/api/search?q=query`

## Authentication

The API uses JWT tokens for authentication. After login, include the token in the Authorization header:
```
Authorization: Bearer <your_token>
```

## Database

PostgreSQL is used for persistent storage. The schema includes:
- Users
- Products
- Categories
- Carts
- Orders
- Delivery Addresses

Database migrations are handled automatically when the container starts.

## Search

Product search is powered by Elasticsearch. Products are automatically indexed when created through the API.

## Environment Variables

Configure these in docker-compose.yml or .env file:
- `POSTGRES_USER`: Database username
- `POSTGRES_PASSWORD`: Database password
- `POSTGRES_DB`: Database name
- `JWT_SECRET_KEY`: Secret for JWT tokens

## Development

To run migrations manually:
```bash
flask db upgrade
```

To rebuild the search index:
```python
from app import db, es
products = Product.query.all()
for product in products:
    es.index(
        index='products',
        id=product.id,
        body={
            'name': product.name,
            'description': product.description,
            'price': float(product.price),
            'category_id': product.category_id
        }
    )
```
