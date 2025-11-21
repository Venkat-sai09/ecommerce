package com.onlineshopping.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSearchRepository extends ElasticsearchRepository<ProductSearchDocument, String> {}

