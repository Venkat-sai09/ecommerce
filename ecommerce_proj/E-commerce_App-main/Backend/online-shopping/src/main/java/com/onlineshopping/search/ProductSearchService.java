package com.onlineshopping.search;
import co.elastic.clients.json.JsonData;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.SearchPage;
import org.springframework.data.elasticsearch.core.SearchHitSupport;
import org.springframework.stereotype.Service;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.aggregations.AggregationRange;

import com.onlineshopping.dao.ProductDao;
import com.onlineshopping.model.Product;

@Service
public class ProductSearchService {

    @Autowired private ProductSearchRepository esRepo;
    @Autowired private ElasticsearchOperations esOps;
    @Autowired private ProductDao productDao;

    public void indexProduct(Product p) { esRepo.save(ProductSearchMapper.from(p)); }
    public void deleteProduct(int id) { esRepo.deleteById(String.valueOf(id)); }

    public long reindexAll() {
        List<Product> all = productDao.findAll();
        esRepo.saveAll(all.stream().map(ProductSearchMapper::from).collect(Collectors.toList()));
        return all.size();
    }

    public SearchPage<ProductSearchDocument> search(String q, String category, String brand,
                                                    Double minPrice, Double maxPrice,
                                                    int page, int size) {

        NativeQuery nativeQuery = NativeQuery.builder()
            .withPageable(PageRequest.of(page, size))
            .withQuery(qb -> qb.bool(b -> {
                if (q != null && !q.isBlank()) {
                    b.should(s -> s.match(m -> m.field("title").query(q).fuzziness("AUTO")));
                    b.should(s -> s.match(m -> m.field("description").query(q).fuzziness("AUTO")));
                    b.minimumShouldMatch("1");
                }
                if (category != null && !category.isBlank()) {
                    b.filter(f -> f.term(t -> t.field("category").value(category)));
                }
                if (brand != null && !brand.isBlank()) {
                    b.filter(f -> f.term(t -> t.field("brand").value(brand)));
                }
                if (minPrice != null) {
                    b.filter(f -> f.range(r -> r.field("price").gte(JsonData.of(minPrice))));
                }
                if (maxPrice != null) {
                    b.filter(f -> f.range(r -> r.field("price").lte(JsonData.of(maxPrice))));
                }
                return b;
            }))
            .withAggregation("by_category",
                Aggregation.of(a -> a.terms(t -> t.field("category").size(20))))
            .withAggregation("by_brand",
                Aggregation.of(a -> a.terms(t -> t.field("brand").size(20))))
            .withAggregation("price_ranges",
                Aggregation.of(a -> a.range(r -> r.field("price").ranges(
                    AggregationRange.of(ar -> ar.to(100.0)),
                    AggregationRange.of(ar -> ar.from(100.0).to(500.0)),
                    AggregationRange.of(ar -> ar.from(500.0).to(2000.0)),
                    AggregationRange.of(ar -> ar.from(2000.0))
                )))
            )
            .build();

        SearchHits<ProductSearchDocument> hits =
            esOps.search(nativeQuery, ProductSearchDocument.class);

        return SearchHitSupport.searchPageFor(hits, nativeQuery.getPageable());
    }

    public List<String> suggest(String prefix) {
        NativeQuery q = NativeQuery.builder()
            .withQuery(b -> b.match(m -> m.field("suggest").query(prefix)))
            .withPageable(PageRequest.of(0, 5))
            .build();

        SearchHits<ProductSearchDocument> hits =
            esOps.search(q, ProductSearchDocument.class);

        return hits.getSearchHits().stream()
            .map(h -> h.getContent().getTitle())        
            .toList();
    }
}
