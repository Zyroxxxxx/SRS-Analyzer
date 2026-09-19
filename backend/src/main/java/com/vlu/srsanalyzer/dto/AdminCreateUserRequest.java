package com.vlu.srsanalyzer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminCreateUserRequest {

    @NotBlank(message = "Ten dang nhap khong duoc de trong")
    @Size(min = 3, max = 50, message = "Ten dang nhap tu 3 den 50 ky tu")
    private String username;

    @NotBlank(message = "Email khong duoc de trong")
    @Email(message = "Email khong hop le")
    private String email;

    @NotBlank(message = "Mat khau khong duoc de trong")
    @Size(min = 6, message = "Mat khau toi thieu 6 ky tu")
    private String password;

    private String fullName;

    // "USER" hoac "ADMIN" - khac voi dang ky cong khai, admin duoc chon quyen ngay khi tao
    private String role;
}
