import Carousel from "./Carousel";
import GetAllCategories from "../productComponent/GetAllCategories";
import ProductCard from "../productComponent/ProductCard";
import { useState, useEffect } from "react";
import { useParams } from "react-router-dom";
import api from "../services/api";
import { API_BASE_URL } from "../config";

const HomePage = () => {
  const [products, setProducts] = useState([]);

  const { categoryId } = useParams();

  useEffect(() => {
    const getAllProducts = async () => {
      const allProducts = await retrieveAllProducts();
      if (allProducts) {
        setProducts(allProducts);
      }
    };

    const getProductsByCategory = async () => {
      const allProducts = await retrieveProductsByCategory();
      if (allProducts) {
        setProducts(allProducts);
      }
    };

    if (categoryId == null) {
      console.log("Category Id is null");
      getAllProducts();
    } else {
      console.log("Category Id is NOT null");
      getProductsByCategory();
    }
  }, [categoryId]);

  const retrieveAllProducts = async () => {
    const response = await api.get("/products");
    console.log('All products response:', response.data);
    return response.data.items;
  };

  const retrieveProductsByCategory = async () => {
    const response = await api.get(`/products/category/${categoryId}`);
    return response.data.items;
  };

  return (
    <div className="container-fluid mb-2">
      <Carousel />
      <div className="mt-2 mb-5">
        <div className="row">
          <div className="col-md-2">
            <GetAllCategories />
          </div>
          <div className="col-md-10">
            <div className="row row-cols-1 row-cols-md-4 g-4">
              
                {products.map((product) => {
                  return <ProductCard item={product} />;
                })}
              </div>
            
          </div>
        </div>
      </div>
    </div>
  );
};

export default HomePage;
