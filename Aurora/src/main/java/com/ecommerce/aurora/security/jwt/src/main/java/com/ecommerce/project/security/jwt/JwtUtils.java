package com.ecommerce.aurora.security.jwt.src.main.java.com.ecommerce.project.security.jwt;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class JwtUtils {

    public String getJwtFromHeader(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
    // Generating Token from Username
    // Getting username from jwt token
    // generate Signing key
    // Validate Jwt token

}
