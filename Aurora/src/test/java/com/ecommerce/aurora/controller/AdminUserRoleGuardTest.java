package com.ecommerce.aurora.controller;

import com.ecommerce.aurora.model.AppRole;
import com.ecommerce.aurora.model.Role;
import com.ecommerce.aurora.model.User;
import com.ecommerce.aurora.repositories.RoleRepository;
import com.ecommerce.aurora.repositories.UserRepository;
import com.ecommerce.aurora.security.request.UpdateRolesRequest;
import com.ecommerce.aurora.security.services.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * This endpoint is itself behind hasRole("ADMIN"), so an admin who removed their own admin role
 * closed the only door back in -- the account could only be restored by editing the database
 * directly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminUserRoleGuardTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void refusesToRemoveTheRoleFromTheOnlyRemainingAdmin() throws Exception {
        User onlyAdmin = persistAdmin("soleAdmin", "soleadmin@example.com");
        // DataInitializer seeds a default admin into the application context, so this account
        // isn't the last one until that one is demoted as well.
        demoteEveryAdminExcept(onlyAdmin);
        authenticateAs(onlyAdmin);

        mockMvc.perform(put("/api/admin/users/" + onlyAdmin.getUserId() + "/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rolesRequest("user"))))
                .andExpect(status().isBadRequest());

        assertThat(isAdmin(onlyAdmin.getUserId())).isTrue();
    }

    @Test
    void allowsDemotingAnAdminWhileAnotherAdminRemains() throws Exception {
        User firstAdmin = persistAdmin("firstAdmin", "firstadmin@example.com");
        User secondAdmin = persistAdmin("secondAdmin", "secondadmin@example.com");
        authenticateAs(firstAdmin);

        mockMvc.perform(put("/api/admin/users/" + secondAdmin.getUserId() + "/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rolesRequest("user"))))
                .andExpect(status().isOk());

        assertThat(isAdmin(secondAdmin.getUserId())).isFalse();
        assertThat(isAdmin(firstAdmin.getUserId())).isTrue();
    }

    @Test
    void stillAllowsPromotingAnOrdinaryUser() throws Exception {
        User admin = persistAdmin("promotingAdmin", "promotingadmin@example.com");
        User ordinary = persistUser("ordinaryUser", "ordinary@example.com");
        authenticateAs(admin);

        mockMvc.perform(put("/api/admin/users/" + ordinary.getUserId() + "/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rolesRequest("admin"))))
                .andExpect(status().isOk());

        assertThat(isAdmin(ordinary.getUserId())).isTrue();
    }

    private UpdateRolesRequest rolesRequest(String... roles) {
        UpdateRolesRequest request = new UpdateRolesRequest();
        request.setRoles(Set.of(roles));
        return request;
    }

    private void demoteEveryAdminExcept(User keptAdmin) {
        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(AppRole.ROLE_USER)));

        userRepository.findAll().stream()
                .filter(candidate -> !candidate.getUserId().equals(keptAdmin.getUserId()))
                .filter(candidate -> candidate.getRoles().stream()
                        .anyMatch(role -> role.getRoleName() == AppRole.ROLE_ADMIN))
                .forEach(candidate -> {
                    // A managed entity's role collection is mutated by Hibernate, so it can't be
                    // replaced with an immutable Set.of(...) the way a fresh entity's can.
                    candidate.setRoles(new HashSet<>(Set.of(userRole)));
                    userRepository.saveAndFlush(candidate);
                });
    }

    private boolean isAdmin(Long userId) {
        return userRepository.findById(userId).orElseThrow().getRoles().stream()
                .anyMatch(role -> role.getRoleName() == AppRole.ROLE_ADMIN);
    }

    private User persistAdmin(String username, String email) {
        Role adminRole = roleRepository.findByRoleName(AppRole.ROLE_ADMIN)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(AppRole.ROLE_ADMIN)));
        User admin = new User(username, "password12345", email);
        admin.setRoles(Set.of(adminRole));
        return userRepository.saveAndFlush(admin);
    }

    private User persistUser(String username, String email) {
        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(AppRole.ROLE_USER)));
        User user = new User(username, "password12345", email);
        user.setRoles(Set.of(userRole));
        return userRepository.saveAndFlush(user);
    }

    private void authenticateAs(User user) {
        UserDetailsImpl principal = UserDetailsImpl.build(user);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
