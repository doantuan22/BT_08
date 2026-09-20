package com.example.bt_08.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductInput(
        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
        String title,

        @NotNull(message = "Giá không được để trống")
        @DecimalMin(value = "0", inclusive = false, message = "Giá phải lớn hơn 0")
        @DecimalMax(value = "9999999999999999.99", message = "Giá quá lớn")
        @Digits(integer = 16, fraction = 2, message = "Giá không hợp lệ (tối đa 16 chữ số nguyên và 2 chữ số thập phân)")
        BigDecimal price,

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 0, message = "Số lượng không được âm")
        Integer quantity,

        @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
        String desc,

        @NotNull(message = "categoryId không được để trống")
        Long categoryId) {
}
