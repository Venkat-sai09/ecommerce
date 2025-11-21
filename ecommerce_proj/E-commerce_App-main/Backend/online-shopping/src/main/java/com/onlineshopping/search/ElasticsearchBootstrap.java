package com.onlineshopping.search;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;

@Configuration
public class ElasticsearchBootstrap {

    @Autowired private ElasticsearchOperations operations;

    @PostConstruct
    public void init() {
        IndexOperations io = operations.indexOps(ProductSearchDocument.class);
        if (!io.exists()) {
            io.create();
            io.putMapping(io.createMapping(ProductSearchDocument.class));
        }
    }
}

