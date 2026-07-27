package com.ecommerce.aurora.security.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;
    @NotBlank
    @Size(min = 3, max = 50)
    private String password;
    @NotBlank
    @Size(min = 3, max = 50)
    @Email
    private String email;
}
