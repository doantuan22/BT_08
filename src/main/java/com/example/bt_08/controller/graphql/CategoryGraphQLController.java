package com.example.bt_08.controller.graphql;

import java.util.List;

import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import com.example.bt_08.dto.CategoryDto;
import com.example.bt_08.dto.CategoryInput;
import com.example.bt_08.service.CategoryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CategoryGraphQLController {

    private final CategoryService categoryService;

    @QueryMapping
    public List<CategoryDto> categories() {
        return categoryService.getAll();
    }

    @QueryMapping
    public CategoryDto category(@Argument Long id) {
        return categoryService.findById(id);
    }

    @MutationMapping
    public CategoryDto createCategory(@Argument @Valid CategoryInput input) {
        return categoryService.create(input);
    }

    @MutationMapping
    public CategoryDto updateCategory(@Argument Long id, @Argument @Valid CategoryInput input) {
        return categoryService.update(id, input);
    }

    @MutationMapping
    public boolean deleteCategory(@Argument Long id) {
        return categoryService.delete(id);
    }
}
