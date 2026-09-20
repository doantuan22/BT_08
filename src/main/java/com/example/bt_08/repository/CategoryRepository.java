package com.example.bt_08.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bt_08.entity.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
