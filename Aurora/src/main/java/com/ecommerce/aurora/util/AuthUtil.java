package com.ecommerce.aurora.util;

import com.ecommerce.aurora.exceptions.ResourceNotFoundException;
import com.ecommerce.aurora.model.AppRole;
import com.ecommerce.aurora.model.User;
import com.ecommerce.aurora.repositories.UserRepository;
import com.ecommerce.aurora.security.services.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthUtil {

    private final UserRepository userRepository;

    public String loggedInEmail() {
        return currentUserDetails().getEmail();
    }

    public Long loggedInUserId() {
        return currentUserDetails().getId();
    }

    public User loggedInUser() {
        String email = loggedInEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    public boolean isCurrentUserAdmin() {
        return currentUserDetails().getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(AppRole.ROLE_ADMIN.name()));
    }

    private UserDetailsImpl currentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (UserDetailsImpl) authentication.getPrincipal();
    }
}
