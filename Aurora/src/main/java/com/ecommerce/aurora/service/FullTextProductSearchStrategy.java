package com.ecommerce.aurora.service;

import com.ecommerce.aurora.model.Product;
import com.ecommerce.aurora.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Collectors;

@Component
@Profile("mysql")
@RequiredArgsConstructor
public class FullTextProductSearchStrategy implements ProductSearchStrategy {

    private final ProductRepository productRepository;
    @Override
    public Page<Product> search(String keyword, Pageable pageable) {
        String booleanModeQuery = toBooleanModeQuery(keyword);
        if(booleanModeQuery.isBlank()) {
            return Page.empty(pageable);
        }
        return productRepository.searchByFullText(booleanModeQuery, pageable);
    }

    /**
     * MySQL boolean-mode operators (+ - * " ( ) ~ < > @) change what the query means
     * if user input reaches AGAINST() with them intact, so every token is stripped
     * of them first, before this method adds its own '+' (required) and trailing '*'
     * (prefix match).
     */
    private String toBooleanModeQuery(String keyword) {
        return Arrays.stream(keyword.trim().split("\\s+"))
                .map(token -> token.replaceAll("[+\\-<>()~*\"@]", ""))
                .filter(token -> !token.isBlank())
                .map(token -> "+" + token + "*")
                .collect(Collectors.joining(" "));
    }
}
