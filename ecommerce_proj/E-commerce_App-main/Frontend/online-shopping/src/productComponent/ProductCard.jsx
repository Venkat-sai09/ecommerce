
import { Link } from "react-router-dom";
import CategoryNavigator from "./CategoryNavigator";

const ProductCard = (product) => {
  console.log('ProductCard received:', product);
  return (
    <div className="col">
    <div class="card border-color rounded-card card-hover product-card custom-bg h-100">
      <img
        class="card-img-top rounded mx-auto d-block m-2"
        alt="img"
        style={{
          maxHeight: "270px",
          maxWidth: "100%",
          width: "auto",
        }}
      />

      <div class="card-body text-color">
        <h5 class="card-title d-flex justify-content-between">
          <div>
            <b>{product.item.name}</b>
          </div>
          <CategoryNavigator
            item={{
              id: product.item.category_id,
              name: product.item.category_name,
            }}
          />
        </h5>
        <p className="card-text">
          <b>{product.item.description}</b>
        </p>
      </div>
      <div class="card-footer">
        <div className="text-center text-color">
          <p>
            <span>
              <h4>Price : &#8377;{product.item.price}</h4>
            </span>
          </p>
        </div>
        <div className="d-flex justify-content-between">
          <Link
            to={`/product/${product.item.id}/category/${product.item.category_id}`}
            className="btn bg-color custom-bg-text"
          >
            Add to Cart
          </Link>

          <p class="text-color">
            <b>
              <i>Stock :</i> {product.item.stock}
            </b>
          </p>
        </div>
      </div>
    </div>
    </div>
  );
};

export default ProductCard;
