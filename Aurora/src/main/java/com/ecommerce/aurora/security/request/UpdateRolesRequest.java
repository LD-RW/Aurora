package com.ecommerce.aurora.security.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateRolesRequest {
    @NotEmpty
    private Set<String> roles;
}
