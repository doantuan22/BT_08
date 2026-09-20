package com.example.bt_08.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bt_08.dto.ProductDto;
import com.example.bt_08.dto.ProductInput;
import com.example.bt_08.entity.Category;
import com.example.bt_08.entity.Product;
import com.example.bt_08.exception.ResourceNotFoundException;
import com.example.bt_08.repository.CategoryRepository;
import com.example.bt_08.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public List<ProductDto> getAllSortedByPrice() {
        return productRepository.findAllByOrderByPriceAsc().stream().map(ProductDto::from).toList();
    }

    public List<ProductDto> getByCategory(Long categoryId) {
        requireCategory(categoryId);
        return productRepository.findByCategoryIdOrderByPriceAsc(categoryId).stream()
                .map(ProductDto::from).toList();
    }

    public List<ProductDto> getByCategoryName(String categoryName) {
        return productRepository.findByCategoryNameIgnoreCaseOrderByPriceAsc(categoryName.trim()).stream()
                .map(ProductDto::from).toList();
    }

    public ProductDto getById(Long id) {
        return ProductDto.from(findEntity(id));
    }

    public ProductDto findById(Long id) {
        return productRepository.findById(id).map(ProductDto::from).orElse(null);
    }

    @Transactional
    public ProductDto create(ProductInput input) {
        Product product = Product.builder()
                .title(input.title().trim())
                .price(input.price())
                .quantity(input.quantity())
                .desc(input.desc())
                .category(requireCategory(input.categoryId()))
                .build();
        return ProductDto.from(productRepository.save(product));
    }

    @Transactional
    public ProductDto update(Long id, ProductInput input) {
        Product product = findEntity(id);
        product.setTitle(input.title().trim());
        product.setPrice(input.price());
        product.setQuantity(input.quantity());
        product.setDesc(input.desc());
        product.setCategory(requireCategory(input.categoryId()));
        return ProductDto.from(product);
    }

    @Transactional
    public boolean delete(Long id) {
        productRepository.delete(findEntity(id));
        return true;
    }

    private Product findEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    private Category requireCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", categoryId));
    }
}
