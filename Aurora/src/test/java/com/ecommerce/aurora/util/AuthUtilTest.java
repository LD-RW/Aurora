package com.ecommerce.aurora.util;

import com.ecommerce.aurora.exceptions.ResourceNotFoundException;
import com.ecommerce.aurora.repositories.UserRepository;
import com.ecommerce.aurora.security.services.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AuthUtilTest {

    @Mock
    private UserRepository userRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void isCurrentUserAdminReturnsTrueWhenTheAuthenticatedPrincipalHasTheAdminAuthority() {
        AuthUtil authUtil = new AuthUtil(userRepository);
        authenticateWithAuthorities("ROLE_ADMIN");

        assertThat(authUtil.isCurrentUserAdmin()).isTrue();
    }

    @Test
    void isCurrentUserAdminReturnsFalseWhenTheAuthenticatedPrincipalLacksTheAdminAuthority() {
        AuthUtil authUtil = new AuthUtil(userRepository);
        authenticateWithAuthorities("ROLE_USER");

        assertThat(authUtil.isCurrentUserAdmin()).isFalse();
    }

    @Test
    void assertOwnerOrAdminDoesNothingWhenTheCallerIsTheOwner() {
        AuthUtil authUtil = new AuthUtil(userRepository);
        authenticateWithAuthorities("ROLE_USER");

        assertThatCode(() -> authUtil.assertOwnerOrAdmin(1L, "Address", "addressId", 10L))
                .doesNotThrowAnyException();
    }

    @Test
    void assertOwnerOrAdminDoesNothingWhenTheCallerIsAnAdminEvenIfNotTheOwner() {
        AuthUtil authUtil = new AuthUtil(userRepository);
        authenticateWithAuthorities("ROLE_ADMIN");

        assertThatCode(() -> authUtil.assertOwnerOrAdmin(2L, "Address", "addressId", 10L))
                .doesNotThrowAnyException();
    }

    @Test
    void assertOwnerOrAdminThrowsNotFoundWhenTheCallerIsNeitherTheOwnerNorAnAdmin() {
        AuthUtil authUtil = new AuthUtil(userRepository);
        authenticateWithAuthorities("ROLE_USER");

        assertThatThrownBy(() -> authUtil.assertOwnerOrAdmin(2L, "Address", "addressId", 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void authenticateWithAuthorities(String... authorities) {
        UserDetailsImpl principal = new UserDetailsImpl(1L, "someone", "someone@example.com", "password",
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
