package com.ecommerce.aurora.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    /**
     * Registers a bearer scheme so Swagger UI's Authorize button has something to attach, but
     * does not apply it globally via addSecurityItem. A global security requirement would mark
     * every operation as needing it -- including the genuinely public ones under /api/public/**
     * and /api/auth/** -- since no controller method here overrides it with its own
     * per-operation @SecurityRequirement. Under-documenting (no lock icons at all until
     * operations are annotated) is the more honest default than over-claiming that public
     * endpoints require auth.
     *
     * The scheme itself is a fallback, not how the API is actually used day to day: sign-in's
     * primary output is an HttpOnly cookie (see JwtUtils.generateJwtCookie), which JavaScript --
     * including Swagger UI's own "Try it out" -- can never read, by design. This bearer scheme
     * exists only so a value copied out of the cookie by hand can be pasted into Authorize,
     * for testing from a context where the browser wouldn't otherwise attach that cookie.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Optional fallback only. Authentication normally happens via the "
                        + "HttpOnly session cookie set by POST /api/auth/signin, which cannot be "
                        + "read or set here. To use this instead, sign in, manually copy the "
                        + "cookie's value from your browser's dev tools, and paste it below.");

        return new OpenAPI()
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", bearerScheme));
    }
}
