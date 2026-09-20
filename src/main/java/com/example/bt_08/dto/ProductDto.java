package com.example.bt_08.dto;

import java.math.BigDecimal;

import com.example.bt_08.entity.Product;

public record ProductDto(
        Long id,
        String title,
        BigDecimal price,
        Integer quantity,
        String desc,
        Long categoryId) {

    public static ProductDto from(Product p) {
        return new ProductDto(p.getId(), p.getTitle(), p.getPrice(), p.getQuantity(),
                p.getDesc(), p.getCategory().getId());
    }
}
