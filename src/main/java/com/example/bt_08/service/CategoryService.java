package com.example.bt_08.service;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.bt_08.dto.CategoryDto;
import com.example.bt_08.dto.CategoryInput;
import com.example.bt_08.entity.Category;
import com.example.bt_08.exception.BusinessException;
import com.example.bt_08.exception.ResourceNotFoundException;
import com.example.bt_08.repository.CategoryRepository;
import com.example.bt_08.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public List<CategoryDto> getAll() {
        return categoryRepository.findAll(Sort.by("name")).stream().map(CategoryDto::from).toList();
    }

    public CategoryDto getById(Long id) {
        return CategoryDto.from(findEntity(id));
    }

    public CategoryDto findById(Long id) {
        return categoryRepository.findById(id).map(CategoryDto::from).orElse(null);
    }

    public List<CategoryDto> getByIds(Collection<Long> ids) {
        return categoryRepository.findAllById(ids).stream().map(CategoryDto::from).toList();
    }

    @Transactional
    public CategoryDto create(CategoryInput input) {
        String name = input.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessException("Category '" + name + "' đã tồn tại");
        }
        Category saved = categoryRepository.save(Category.builder()
                .name(name)
                .images(input.images())
                .build());
        return CategoryDto.from(saved);
    }

    @Transactional
    public CategoryDto update(Long id, CategoryInput input) {
        Category category = findEntity(id);
        String name = input.name().trim();
        if (categoryRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessException("Category '" + name + "' đã tồn tại");
        }
        category.setName(name);
        category.setImages(input.images());
        return CategoryDto.from(category);
    }

    @Transactional
    public boolean delete(Long id) {
        Category category = findEntity(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new BusinessException("Không thể xoá category '" + category.getName()
                    + "' vì vẫn còn sản phẩm thuộc category này");
        }
        categoryRepository.delete(category);
        return true;
    }

    private Category findEntity(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
    }
}
