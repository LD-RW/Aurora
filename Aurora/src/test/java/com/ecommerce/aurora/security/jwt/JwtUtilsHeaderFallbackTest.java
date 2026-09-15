package com.ecommerce.aurora.security.jwt;

import com.ecommerce.aurora.model.AppRole;
import com.ecommerce.aurora.model.Role;
import com.ecommerce.aurora.model.User;
import com.ecommerce.aurora.repositories.RoleRepository;
import com.ecommerce.aurora.repositories.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthTokenFilter.parseJwt checks the cookie first and only falls back to the Authorization
 * header when no cookie is present -- this is what makes the header a fallback rather than a
 * second, equally-trusted credential source. Every case here goes through the real filter chain
 * (a genuine @SpringBootTest request), not a SecurityContextHolder shortcut, since the thing
 * under test is the filter's own parsing order.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class JwtUtilsHeaderFallbackTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${spring.app.jwtCookieName}")
    private String cookieName;

    @Test
    void authenticatesViaTheAuthorizationHeaderWhenNoCookieIsPresent() throws Exception {
        String token = jwtUtils.generateTokenFromUsername(persistUser("headerOnlyUser").getUsername());

        mockMvc.perform(get("/api/auth/user").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("headerOnlyUser"));
    }

    @Test
    void prefersTheCookieOverTheHeaderWhenBothArePresent() throws Exception {
        String cookieToken = jwtUtils.generateTokenFromUsername(persistUser("cookieUser").getUsername());
        String headerToken = jwtUtils.generateTokenFromUsername(persistUser("headerUser").getUsername());

        mockMvc.perform(get("/api/auth/user")
                        .cookie(new Cookie(cookieName, cookieToken))
                        .header("Authorization", "Bearer " + headerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("cookieUser"));
    }

    @Test
    void rejectsTheRequestWhenNeitherCookieNorHeaderIsPresent() throws Exception {
        mockMvc.perform(get("/api/auth/user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAMalformedBearerTokenTheSameAsAMissingOne() throws Exception {
        mockMvc.perform(get("/api/auth/user").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    private User persistUser(String username) {
        Role userRole = roleRepository.findByRoleName(AppRole.ROLE_USER)
                .orElseGet(() -> roleRepository.saveAndFlush(new Role(AppRole.ROLE_USER)));
        User user = new User(username, "password12345", username + "@example.com");
        user.setRoles(Set.of(userRole));
        return userRepository.saveAndFlush(user);
    }
}
