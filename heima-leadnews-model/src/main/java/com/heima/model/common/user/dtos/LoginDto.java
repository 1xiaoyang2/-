package com.heima.model.common.user.dtos;


import lombok.Data;

@Data
public class LoginDto {
    private String phone;

    private String password;
}
