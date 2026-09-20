package com.example.bt_08.controller.graphql;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.BatchMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.example.bt_08.dto.CategoryDto;
import com.example.bt_08.dto.ProductDto;
import com.example.bt_08.dto.ProductInput;
import com.example.bt_08.service.CategoryService;
import com.example.bt_08.service.ProductService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ProductGraphQLController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @QueryMapping
    public List<ProductDto> products() {
        return productService.getAllSortedByPrice();
    }

    @QueryMapping
    public List<ProductDto> productsByCategory(@Argument Long categoryId) {
        return productService.getByCategory(categoryId);
    }

    @QueryMapping
    public List<ProductDto> productsByCategoryName(@Argument String categoryName) {
        return productService.getByCategoryName(categoryName);
    }

    @QueryMapping
    public ProductDto product(@Argument Long id) {
        return productService.findById(id);
    }

    @MutationMapping
    public ProductDto createProduct(@Argument @Valid ProductInput input) {
        return productService.create(input);
    }

    @MutationMapping
    public ProductDto updateProduct(@Argument Long id, @Argument @Valid ProductInput input) {
        return productService.update(id, input);
    }

    @MutationMapping
    public boolean deleteProduct(@Argument Long id) {
        return productService.delete(id);
    }

    @BatchMapping(typeName = "Product", field = "category")
    public Map<ProductDto, CategoryDto> category(List<ProductDto> products) {
        List<Long> ids = products.stream().map(ProductDto::categoryId).distinct().toList();
        Map<Long, CategoryDto> byId = categoryService.getByIds(ids).stream()
                .collect(Collectors.toMap(CategoryDto::id, Function.identity()));
        return products.stream()
                .collect(Collectors.toMap(Function.identity(), p -> byId.get(p.categoryId()), (a, b) -> a));
    }
}
