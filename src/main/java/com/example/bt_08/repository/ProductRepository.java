package com.example.bt_08.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bt_08.entity.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = "category")
    List<Product> findAllByOrderByPriceAsc();

    @EntityGraph(attributePaths = "category")
    List<Product> findByCategoryIdOrderByPriceAsc(Long categoryId);

    @EntityGraph(attributePaths = "category")
    List<Product> findByCategoryId(Long categoryId);

    @EntityGraph(attributePaths = "category")
    List<Product> findByCategoryNameIgnoreCaseOrderByPriceAsc(String categoryName);

    boolean existsByCategoryId(Long categoryId);
}
