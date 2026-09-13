package com.ecommerce.aurora.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * No Access-Control-* header was returned at all before this, so a browser rejected every
 * cross-origin call before the response was ever read. The preflight was doubly blocked: with no
 * CORS configuration the OPTIONS request also fell under "anyRequest().authenticated()".
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigurationTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void answersAPreflightFromAnAllowedOriginWithoutRequiringAuthentication() throws Exception {
        mockMvc.perform(options("/api/public/products")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }

    @Test
    void allowsCredentialsSoTheAuthenticationCookieIsSentOnCrossOriginCalls() throws Exception {
        mockMvc.perform(options("/api/public/products")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    void echoesTheAllowedOriginOnAnActualRequest() throws Exception {
        mockMvc.perform(get("/api/public/products").header("Origin", ALLOWED_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN));
    }

    @Test
    void refusesAnOriginThatIsNotOnTheAllowList() throws Exception {
        mockMvc.perform(options("/api/public/products")
                        .header("Origin", "https://not-our-frontend.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
