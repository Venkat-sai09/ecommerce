from flask import Flask, jsonify, request, abort
from flask_sqlalchemy import SQLAlchemy
from flask_migrate import Migrate
from flask_jwt_extended import JWTManager, create_access_token, jwt_required, get_jwt_identity
from flask_cors import CORS
import os
from dotenv import load_dotenv
from elasticsearch import Elasticsearch
from datetime import timedelta

load_dotenv()

app = Flask(__name__)
CORS(app, resources={
    r"/*": {
        "origins": ["http://localhost:3000"],  # React frontend URL
        "methods": ["GET", "POST", "PUT", "DELETE", "OPTIONS"],
        "allow_headers": ["Content-Type", "Authorization"]
    }
})

# Database configuration
DB_USER = os.getenv('POSTGRES_USER', 'postgres')
DB_PASS = os.getenv('POSTGRES_PASSWORD', 'postgres')
DB_NAME = os.getenv('POSTGRES_DB', 'ecommerce')
DB_HOST = os.getenv('POSTGRES_HOST', 'localhost')
DB_PORT = os.getenv('POSTGRES_PORT', '5432')

app.config['SQLALCHEMY_DATABASE_URI'] = f'postgresql://{DB_USER}:{DB_PASS}@{DB_HOST}:{DB_PORT}/{DB_NAME}'
app.config['SQLALCHEMY_TRACK_MODIFICATIONS'] = False
app.config['JWT_SECRET_KEY'] = os.getenv('JWT_SECRET_KEY', 'your-secret-key')  # Change in production
app.config['JWT_ACCESS_TOKEN_EXPIRES'] = timedelta(hours=24)

# Initialize extensions
db = SQLAlchemy(app)
migrate = Migrate(app, db)
jwt = JWTManager(app)

# Import models after db is initialized
from models import User, Product, Category, Cart, CartItem, Order, OrderItem, DeliveryAddress

# Elasticsearch setup
es = Elasticsearch([{'host': 'elasticsearch', 'port': 9200, 'scheme': 'http'}])

# API Routes

@app.route('/')
def index():
    return jsonify({'message': 'Flask backend for E-commerce app is running'})

@app.route('/health')
def health():
    try:
        from sqlalchemy import text
        db.session.execute(text('SELECT 1'))
        es_health = es.cluster.health()
        return jsonify({
            'db': 'ok',
            'elasticsearch': es_health['status']
        })
    except Exception as e:
        return jsonify({'error': str(e)}), 500

# User routes
@app.route('/api/auth/register', methods=['POST'])
def register():
    data = request.get_json()
    
    if User.query.filter_by(email=data['email']).first():
        return jsonify({'error': 'Email already registered'}), 400
        
    user = User(
        username=data['username'],
        email=data['email'],
        role=data.get('role', 'user')
    )
    user.set_password(data['password'])
    
    db.session.add(user)
    db.session.commit()
    
    return jsonify(user.to_dict()), 201

@app.route('/api/auth/login', methods=['POST'])
def login():
    data = request.get_json()
    user = User.query.filter_by(email=data['email']).first()
    
    if user and user.check_password(data['password']):
        access_token = create_access_token(identity=user.id)
        return jsonify({
            'token': access_token,
            'user': user.to_dict()
        })
    
    return jsonify({'error': 'Invalid credentials'}), 401

# Product routes
@app.route('/api/products', methods=['GET'])
def get_products():
    page = request.args.get('page', 1, type=int)
    per_page = request.args.get('per_page', 10, type=int)
    category_id = request.args.get('category_id', type=int)
    
    query = Product.query
    if category_id:
        query = query.filter_by(category_id=category_id)
        
    products = query.paginate(page=page, per_page=per_page)
    
    return jsonify({
        'items': [p.to_dict() for p in products.items],
        'total': products.total,
        'pages': products.pages,
        'current_page': products.page
    })

@app.route('/api/products/<int:product_id>', methods=['GET'])
def get_product(product_id):
    product = Product.query.get_or_404(product_id)
    return jsonify(product.to_dict())

@app.route('/api/products', methods=['POST'])
@jwt_required()
def create_product():
    user = User.query.get_or_404(get_jwt_identity())
    if user.role != 'admin':
        abort(403)
        
    data = request.get_json()
    product = Product(
        name=data['name'],
        description=data['description'],
        price=data['price'],
        stock=data['stock'],
        category_id=data['category_id']
    )
    
    db.session.add(product)
    db.session.commit()
    
    # Index in Elasticsearch
    es.index(index='products', id=product.id, body={
        'name': product.name,
        'description': product.description,
        'price': float(product.price),
        'category_id': product.category_id
    })
    
    return jsonify(product.to_dict()), 201

# Category routes
@app.route('/api/categories', methods=['GET'])
def get_categories():
    categories = Category.query.all()
    return jsonify([c.to_dict() for c in categories])

@app.route('/api/categories', methods=['POST'])
@jwt_required()
def create_category():
    user = User.query.get_or_404(get_jwt_identity())
    if user.role != 'admin':
        abort(403)
        
    data = request.get_json()
    category = Category(
        name=data['name'],
        description=data.get('description')
    )
    
    db.session.add(category)
    db.session.commit()
    
    return jsonify(category.to_dict()), 201

# Cart routes
@app.route('/api/cart', methods=['GET'])
@jwt_required()
def get_cart():
    user = User.query.get_or_404(get_jwt_identity())
    cart = user.cart
    
    if not cart:
        cart = Cart(user_id=user.id)
        db.session.add(cart)
        db.session.commit()
    
    return jsonify(cart.to_dict())

@app.route('/api/cart/items', methods=['POST'])
@jwt_required()
def add_to_cart():
    user = User.query.get_or_404(get_jwt_identity())
    cart = user.cart or Cart(user_id=user.id)
    
    if not user.cart:
        db.session.add(cart)
        db.session.commit()
    
    data = request.get_json()
    product = Product.query.get_or_404(data['product_id'])
    
    cart_item = CartItem.query.filter_by(
        cart_id=cart.id,
        product_id=product.id
    ).first()
    
    if cart_item:
        cart_item.quantity += data.get('quantity', 1)
    else:
        cart_item = CartItem(
            cart_id=cart.id,
            product_id=product.id,
            quantity=data.get('quantity', 1)
        )
        db.session.add(cart_item)
    
    db.session.commit()
    return jsonify(cart.to_dict())

# Order routes
@app.route('/api/orders', methods=['POST'])
@jwt_required()
def create_order():
    user = User.query.get_or_404(get_jwt_identity())
    cart = user.cart
    
    if not cart or not cart.items:
        return jsonify({'error': 'Cart is empty'}), 400
        
    data = request.get_json()
    
    # Create order
    order = Order(
        user_id=user.id,
        total_amount=sum(item.product.price * item.quantity for item in cart.items),
        status='pending'
    )
    
    # Add order items
    for cart_item in cart.items:
        order_item = OrderItem(
            product_id=cart_item.product_id,
            quantity=cart_item.quantity,
            price=cart_item.product.price
        )
        order.items.append(order_item)
    
    # Add delivery address
    address = DeliveryAddress(
        street=data['address']['street'],
        city=data['address']['city'],
        state=data['address']['state'],
        postal_code=data['address']['postal_code'],
        country=data['address']['country']
    )
    order.delivery_address = address
    
    # Clear cart
    db.session.query(CartItem).filter_by(cart_id=cart.id).delete()
    
    db.session.add(order)
    db.session.commit()
    
    return jsonify(order.to_dict()), 201

@app.route('/api/orders', methods=['GET'])
@jwt_required()
def get_orders():
    user = User.query.get_or_404(get_jwt_identity())
    orders = Order.query.filter_by(user_id=user.id).all()
    return jsonify([order.to_dict() for order in orders])

@app.route('/api/orders/<int:order_id>', methods=['GET'])
@jwt_required()
def get_order(order_id):
    user = User.query.get_or_404(get_jwt_identity())
    order = Order.query.filter_by(id=order_id, user_id=user.id).first_or_404()
    return jsonify(order.to_dict())

# Search routes
@app.route('/api/search', methods=['GET'])
def search_products():
    query = request.args.get('q', '')
    try:
        response = es.search(index='products', body={
            'query': {
                'multi_match': {
                    'query': query,
                    'fields': ['name^3', 'description']
                }
            }
        })
        
        hits = response['hits']['hits']
        product_ids = [hit['_id'] for hit in hits]
        
        # Get full product details from database
        products = Product.query.filter(Product.id.in_(product_ids)).all()
        return jsonify([p.to_dict() for p in products])
        
    except Exception as e:
        return jsonify({'error': str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
