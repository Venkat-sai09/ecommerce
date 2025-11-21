// package com.onlineshopping.search;

// import java.util.Optional;
// import com.onlineshopping.model.Product;

// public class ProductSearchMapper {
//     public static ProductSearchDocument from(Product p) {
//         ProductSearchDocument d = new ProductSearchDocument();
//         d.setId(String.valueOf(p.getId()));
//         // The following getters may differ in your entity; adjust if needed
//         try { d.setTitle((String) Product.class.getMethod("getTitle").invoke(p)); } catch (Exception e) {
//             try { d.setTitle((String) Product.class.getMethod("getName").invoke(p)); } catch (Exception ex) { d.setTitle("Product "+p.getId()); }
//         }
//         try { d.setDescription((String) Product.class.getMethod("getDescription").invoke(p)); } catch (Exception e) { d.setDescription(""); }
//         try { d.setBrand(Optional.ofNullable((String) Product.class.getMethod("getBrand").invoke(p)).orElse("unknown")); } catch (Exception e) { d.setBrand("unknown"); }
//         try { Object cat = Product.class.getMethod("getCategory").invoke(p);
//               String catTitle = (cat!=null) ? (String) cat.getClass().getMethod("getTitle").invoke(cat) : "Uncategorized";
//               d.setCategory(catTitle);
//         } catch (Exception e) { d.setCategory("Uncategorized"); }
//         try { d.setPrice((java.math.BigDecimal) Product.class.getMethod("getPrice").invoke(p)); } catch (Exception e) { d.setPrice(java.math.BigDecimal.ZERO); }
//         try { d.setRating((Integer) Product.class.getMethod("getRating").invoke(p)); } catch (Exception e) { d.setRating(0); }
//         try { Integer stock = (Integer) Product.class.getMethod("getStock").invoke(p);
//               d.setInStock(stock != null && stock > 0);
//         } catch (Exception e) { d.setInStock(true); }
//         d.setSuggest(d.getTitle());
//         return d;
//     }
// }

package com.onlineshopping.search;

import com.onlineshopping.model.Product;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Optional;

public class ProductSearchMapper {

    public static ProductSearchDocument from(Product p) {
        ProductSearchDocument d = new ProductSearchDocument();

        // ID
        d.setId(String.valueOf(p.getId()));

        // Title / Description (adjust if your getters differ)
        d.setTitle(safeInvokeString(p, "getTitle")
                .orElseGet(() -> safeInvokeString(p, "getName").orElse(null)));

        d.setDescription(safeInvokeString(p, "getDescription").orElse(null));

        // Category: try common names on the Category object; fall back to ID or toString
        Object categoryObj = safeInvoke(p, "getCategory").orElse(null);
        String categoryValue = null;
        if (categoryObj != null) {
            categoryValue = tryCategoryName(categoryObj)
                    .orElseGet(() -> tryCategoryId(categoryObj)
                    .orElse(categoryObj.toString()));
        }
        d.setCategory(categoryValue);

        // Brand: try common possibilities on Product; else null
        String brand = safeInvokeString(p, "getBrand")
                .orElseGet(() -> safeInvokeString(p, "getManufacturer")
                .orElseGet(() -> safeInvokeString(p, "getMaker").orElse(null)));
        d.setBrand(brand);

        // Price: handle BigDecimal or Double
        Double price = null;
        Object priceObj = safeInvoke(p, "getPrice").orElse(null);
        if (priceObj instanceof BigDecimal bd) {
            price = bd.doubleValue();
        } else if (priceObj instanceof Number n) {
            price = n.doubleValue();
        }
        d.setPrice(price);

        // Suggest: use title as a simple suggestion source
        d.setSuggest(d.getTitle());

        return d;
    }

    // ===== Helpers =====

    private static Optional<String> tryCategoryName(Object category) {
        return safeInvokeString(category, "getName")
                .or(() -> safeInvokeString(category, "getCategoryName"))
                .or(() -> safeInvokeString(category, "getTitle"))
                .or(() -> safeInvokeString(category, "getLabel"))
                .or(() -> safeInvokeString(category, "getType"));
    }

    private static Optional<String> tryCategoryId(Object category) {
        return safeInvoke(category, "getId").map(Object::toString);
    }

    private static Optional<Object> safeInvoke(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            m.setAccessible(true);
            return Optional.ofNullable(m.invoke(target));
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private static Optional<String> safeInvokeString(Object target, String methodName) {
        return safeInvoke(target, methodName).map(obj -> obj != null ? String.valueOf(obj) : null);
    }
}

