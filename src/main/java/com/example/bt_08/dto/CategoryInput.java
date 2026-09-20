package com.example.bt_08.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryInput(
        @NotBlank(message = "Tên category không được để trống")
        @Size(max = 100, message = "Tên category tối đa 100 ký tự")
        String name,

        @Size(max = 500, message = "Đường dẫn ảnh tối đa 500 ký tự")
        String images) {
}
