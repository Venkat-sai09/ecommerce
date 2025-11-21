package com.onlineshopping.search;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin
@RequestMapping("/api/search")
public class ProductSearchController {

    @Autowired private ProductSearchService service;

    @GetMapping("/reindex")
    public String reindex() {
        long n = service.reindexAll();
        return "Reindexed " + n + " products";
    }

    @GetMapping
    public SearchPage<ProductSearchDocument> search(
            @RequestParam(required=false) String q,
            @RequestParam(required=false) String category,
            @RequestParam(required=false) String brand,
            @RequestParam(required=false) Double minPrice,
            @RequestParam(required=false) Double maxPrice,
            @RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="12") int size
    ) {
        return service.search(q, category, brand, minPrice, maxPrice, page, size);
    }

    @GetMapping("/suggest")
    public List<String> suggest(@RequestParam String q) {
        return service.suggest(q);
    }
}
