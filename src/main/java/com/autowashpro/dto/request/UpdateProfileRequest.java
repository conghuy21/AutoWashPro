package com.autowashpro.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {

    @NotBlank(message = "Họ và tên không được bỏ trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được bỏ trống")
    @Pattern(regexp = "^[0-9]{9,11}$", message = "Số điện thoại phải có từ 9 đến 11 chữ số")
    private String phone;
}
