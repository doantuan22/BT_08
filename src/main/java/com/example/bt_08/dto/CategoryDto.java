package com.example.bt_08.dto;

import com.example.bt_08.entity.Category;

public record CategoryDto(Long id, String name, String images) {

    public static CategoryDto from(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getImages());
    }
}
