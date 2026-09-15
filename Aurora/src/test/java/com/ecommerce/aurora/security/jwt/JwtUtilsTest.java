package com.ecommerce.aurora.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * getJwtFromHeader is the fallback path AuthTokenFilter reaches for only when no cookie is
 * present -- see JwtUtilsHeaderFallbackTest for that precedence, verified end to end. This class
 * covers the parsing rules of the header itself in isolation.
 */
class JwtUtilsTest {

    private final JwtUtils jwtUtils = new JwtUtils();

    @Test
    void returnsNullWhenNoAuthorizationHeaderIsPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(jwtUtils.getJwtFromHeader(request)).isNull();
    }

    @Test
    void returnsNullWhenTheHeaderDoesNotUseTheBearerScheme() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        assertThat(jwtUtils.getJwtFromHeader(request)).isNull();
    }

    @Test
    void extractsTheTokenAfterTheBearerPrefix() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer some.jwt.token");

        assertThat(jwtUtils.getJwtFromHeader(request)).isEqualTo("some.jwt.token");
    }
}
