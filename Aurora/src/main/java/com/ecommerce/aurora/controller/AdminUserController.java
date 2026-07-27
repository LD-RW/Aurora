package com.ecommerce.aurora.controller;

import com.ecommerce.aurora.exceptions.APIException;
import com.ecommerce.aurora.exceptions.ResourceNotFoundException;
import com.ecommerce.aurora.model.AppRole;
import com.ecommerce.aurora.model.Role;
import com.ecommerce.aurora.model.User;
import com.ecommerce.aurora.repositories.RoleRepository;
import com.ecommerce.aurora.repositories.UserRepository;
import com.ecommerce.aurora.security.request.AdminCreateUserRequest;
import com.ecommerce.aurora.security.request.UpdateRolesRequest;
import com.ecommerce.aurora.security.response.MessageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping
    public ResponseEntity<MessageResponse> createUser(@Valid @RequestBody AdminCreateUserRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Username is already in use"));
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(new MessageResponse("Email is already in use"));
        }

        User user = new User(request.getUsername(), passwordEncoder.encode(request.getPassword()), request.getEmail());
        user.setRoles(resolveRoles(request.getRoles()));
        userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("User created successfully"));
    }

    @PutMapping("/{userId}/roles")
    public ResponseEntity<MessageResponse> updateRoles(@PathVariable Long userId, @Valid @RequestBody UpdateRolesRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "userId", userId));

        user.setRoles(resolveRoles(request.getRoles()));
        userRepository.save(user);

        return ResponseEntity.ok(new MessageResponse("Roles updated successfully"));
    }

    private Set<Role> resolveRoles(Set<String> roleNames) {
        return roleNames.stream()
                .map(this::resolveRole)
                .collect(Collectors.toSet());
    }

    private Role resolveRole(String roleName) {
        AppRole appRole = switch (roleName.toLowerCase()) {
            case "admin" -> AppRole.ROLE_ADMIN;
            case "seller" -> AppRole.ROLE_SELLER;
            case "user" -> AppRole.ROLE_USER;
            default -> throw new APIException("Unknown role: " + roleName);
        };
        return roleRepository.findByRoleName(appRole)
                .orElseThrow(() -> new APIException("Role not found: " + appRole));
    }
}
